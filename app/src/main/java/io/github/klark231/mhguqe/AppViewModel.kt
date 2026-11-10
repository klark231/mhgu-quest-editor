package io.github.klark231.mhguqe

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

// folders a quest or map file can sit in, everything else in a romfs gets skipped
private val GAME_DIRS = Regex("""(?i)romfs|nativeNX|loc|arc|quest|stage|mapUI|eng|fre|ger|ita|spa|chT|chS|jpn|m\d\d\w*""")

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("editor", Context.MODE_PRIVATE)

    var folderName: String?
        get() = sp.getString("folder", null)
        set(v) = sp.edit().putString("folder", v).apply()

    // "vq\turi" per line, a list can show up more than once in a dump
    var lists: List<Pair<String, String>>
        get() = (sp.getString("lists", "") ?: "").lines().filter { '\t' in it }.map { it.substringBefore('\t') to it.substringAfter('\t') }
        set(v) = sp.edit().putString("lists", v.joinToString("\n") { "${it.first}\t${it.second}" }).apply()

    var autoUpdate: Boolean
        get() = sp.getBoolean("auto_update", true)
        set(v) = sp.edit().putBoolean("auto_update", v).apply()

    // -1 = follow the app language
    var questLang: Int
        get() = sp.getInt("quest_lang", -1)
        set(v) = sp.edit().putInt("quest_lang", v).apply()

    // only used below Android 13, newer versions keep it per app themselves
    var uiLang: String
        get() = sp.getString("ui_lang", "") ?: ""
        set(v) = sp.edit().putString("ui_lang", v).apply()

    var stageName: String?
        get() = sp.getString("stage", null)
        set(v) = sp.edit().putString("stage", v).apply()

    // newest first, "uri\tname" per line
    var recent: List<Pair<String, String>>
        get() = (sp.getString("recent", "") ?: "").lines().filter { '\t' in it }.map { it.substringBefore('\t') to it.substringAfter('\t') }
        set(v) = sp.edit().putString("recent", v.joinToString("\n") { "${it.first}\t${it.second}" }).apply()
}

sealed class Doc(val uri: Uri, val name: String) {
    // firstId: the id when it was opened, tells which quest list it lives in if the id gets changed
    class Quest(uri: Uri, name: String, val q: QuestArc, val firstId: Int = q.data.id) : Doc(uri, name)
    class Data(uri: Uri, name: String, val data: QuestData) : Doc(uri, name)
    // arc is null when it came from a bare .ape file
    class Arena(uri: Uri, name: String, val arc: Arc?, val ac: AcEquip) : Doc(uri, name)
}

enum class Screen { HOME, EDITOR, SETTINGS }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    val prefs = Prefs(app)
    val game by lazy { GameData(app) }

    var screen by mutableStateOf(Screen.HOME)
    var doc by mutableStateOf<Doc?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var dirty by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    var recent by mutableStateOf(prefs.recent)
        private set
    var folderName by mutableStateOf(prefs.folderName)
        private set
    private val indexFile get() = File(ctx.filesDir, "quests.tsv")
    var quests by mutableStateOf(runCatching { QuestIndex.load(indexFile.readText()) }.getOrDefault(emptyList()))
        private set
    var autoUpdate by mutableStateOf(prefs.autoUpdate)
        private set
    var questLang by mutableIntStateOf(prefs.questLang)
        private set

    fun changeQuestLang(v: Int) {
        prefs.questLang = v
        questLang = v
    }

    // area maps decoded from the user's own stage files, kept as png in app storage
    private val mapDir get() = File(ctx.filesDir, "maps")
    var mapCount by mutableIntStateOf(mapDir.list()?.size ?: 0)
        private set
    var stageName by mutableStateOf(prefs.stageName)
        private set
    private val mapCache = HashMap<String, ImageBitmap?>()

    fun mapImage(map: Int, area: Int): ImageBitmap? {
        val key = "m%02d_a%02d".format(map, area)
        return mapCache.getOrPut(key) {
            val f = File(mapDir, "$key.png")
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
        }
    }

    /** reads every mapUI area arc from a stage folder or a zip of it */
    fun importStage(uri: Uri, tree: Boolean) = work {
        val n = withContext(Dispatchers.IO) {
            val found = ArrayList<Pair<String, () -> ByteArray>>()
            if (tree) {
                keep(uri)
                walkTree(uri, prune = true) { p, doc -> if (StageMaps.areaOf(p) != null) found += p to { read(doc) } }
            } else {
                // zip entries can only be read in order, so keep the bytes of the arcs we want
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { z ->
                        while (true) {
                            val e = z.nextEntry ?: break
                            if (!e.isDirectory && StageMaps.areaOf(e.name) != null) { val b = z.readBytes(); found += e.name to { b } }
                        }
                    }
                } ?: throw IOException("can't read ${nameOf(uri)}")
            }
            saveMaps(found)
        }
        mapCache.clear()
        mapCount = mapDir.list()?.size ?: 0
        val name = nameOf(uri)
        prefs.stageName = name
        stageName = name
        if (n == 0) throw IOException(ctx.getString(R.string.err_no_maps))
        say(R.string.msg_maps_loaded, mapCount)
    }

    fun clearMaps() {
        mapDir.deleteRecursively()
        mapCache.clear()
        mapCount = 0
        prefs.stageName = null
        stageName = null
    }

    // plain text lists so rewards and supplies can be shared or pasted into other quests
    fun exportText(to: Uri, text: String) = work {
        withContext(Dispatchers.IO) { write(to, text.toByteArray()) }
        say(R.string.msg_saved, nameOf(to))
    }

    fun importText(from: Uri, apply: (String) -> Unit) = work {
        val t = withContext(Dispatchers.IO) { read(from).toString(Charsets.UTF_8) }
        edit { apply(t) }
    }

    // bumped on every edit, screens read it so they redraw
    var rev by mutableIntStateOf(0)
        private set

    fun edit(block: () -> Unit) {
        block()
        rev++
        dirty = true
    }

    private fun say(@StringRes id: Int, vararg args: Any) { message = ctx.getString(id, *args) }

    fun nameOf(uri: Uri): String {
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { return it }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }

    private fun keep(uri: Uri) {
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (_: SecurityException) {
            // came from "open with", we only get it for this session
        }
    }

    private fun read(uri: Uri): ByteArray =
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("can't read ${nameOf(uri)}")

    private fun write(uri: Uri, b: ByteArray) {
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(b) } ?: throw IOException("can't write ${nameOf(uri)}")
    }

    private fun work(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            } finally {
                busy = false
            }
        }
    }

    // ---- backups: "<file>.bak" copies live in app storage, a picked file has no folder we could write next to
    private val backupDir get() = File(ctx.filesDir, "backups")
    private fun backupFile(d: Doc) =
        File(backupDir, "%08x_%s.bak".format(d.uri.toString().hashCode(), d.name.replace(Regex("[^A-Za-z0-9._-]"), "_")))

    /** when the open file's backup was made, null if there is none */
    var backupTime by mutableStateOf<Long?>(null)
        private set

    private fun refreshBackup() {
        backupTime = doc?.let { backupFile(it).takeIf { f -> f.exists() }?.lastModified() }
    }

    fun backupDate(): String = backupTime?.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: ""

    /** copies the file as it is on disk (not the unsaved edits) */
    fun backup() {
        val d = doc ?: return
        work {
            withContext(Dispatchers.IO) {
                val f = backupFile(d)
                f.parentFile?.mkdirs()
                f.writeBytes(read(d.uri))
            }
            refreshBackup()
            say(R.string.msg_backed_up, d.name + ".bak")
        }
    }

    fun restore() {
        val d = doc ?: return
        work {
            val f = backupFile(d)
            val restored = withContext(Dispatchers.IO) {
                val b = f.readBytes()
                val nd = load(d.uri, d.name, b)  // parse first, a broken backup must not overwrite the file
                write(d.uri, b)
                nd
            }
            doc = restored
            dirty = false
            rev++
            say(R.string.msg_restored, d.name)
        }
    }

    // the first save over a file keeps the original, later saves leave that backup alone
    private fun autoBackup(d: Doc) {
        val f = backupFile(d)
        if (f.exists()) return
        runCatching {
            f.parentFile?.mkdirs()
            f.writeBytes(read(d.uri))
        }
    }

    fun open(uri: Uri) = work {
        keep(uri)
        val name = nameOf(uri)
        val d = withContext(Dispatchers.IO) { load(uri, name, read(uri)) }
        doc = d
        refreshBackup()
        dirty = false
        rev++
        addRecent(uri, name)
        screen = Screen.EDITOR
    }

    private fun load(uri: Uri, name: String, b: ByteArray): Doc {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".ape") -> Doc.Arena(uri, name, null, AcEquip.read(b))
            lower.endsWith(".ext") || lower.endsWith(".mib") -> Doc.Data(uri, name, QuestData(b))
            else -> {
                if (b.size >= 4 && b.s32(0) == QuestData.MAGIC) return Doc.Data(uri, name, QuestData(b))
                val arc = Arc.read(b)
                val ape = arc.find(AcEquip.ARC_PATH)
                when {
                    arc.firstByExt(".qdl") != null -> Doc.Quest(uri, name, QuestArc(arc))
                    ape != null -> Doc.Arena(uri, name, arc, AcEquip.read(ape.bytes()))
                    else -> throw IOException(ctx.getString(R.string.err_not_quest))
                }
            }
        }
    }

    private fun addRecent(uri: Uri, name: String) {
        val list = (listOf(uri.toString() to name) + recent.filter { it.first != uri.toString() }).take(10)
        recent = list
        prefs.recent = list
    }

    fun forget(uri: String) {
        recent = recent.filter { it.first != uri }
        prefs.recent = recent
    }

    fun close() {
        doc = null
        dirty = false
        screen = Screen.HOME
    }

    private fun bytesFor(d: Doc, log: (String) -> Unit): ByteArray = when (d) {
        is Doc.Quest -> d.q.save(log)
        is Doc.Data -> d.data.raw.copyOf()
        is Doc.Arena -> {
            val ape = d.ac.write()
            if (d.arc == null) ape else {
                val e = d.arc.find(AcEquip.ARC_PATH)
                if (e != null) e.set(ape) else d.arc.add(AcEquip.ARC_PATH, "rAcPlayerEquip", ape)
                d.arc.write()
            }
        }
    }

    /** to = null saves over the opened file */
    fun save(to: Uri? = null) {
        val d = doc ?: return
        work {
            val target = to ?: d.uri
            if (to != null) keep(to)
            val listMsg = withContext(Dispatchers.IO) {
                val bytes = bytesFor(d) { }
                if (to == null) autoBackup(d)
                write(target, bytes)
                if (d is Doc.Quest && autoUpdate && folderName != null) pushToList(d) else null
            }
            dirty = false
            rev++
            refreshBackup()
            if (to != null) addRecent(to, nameOf(to))
            message = listOf(ctx.getString(R.string.msg_saved, nameOf(target)), listMsg).filterNotNull().joinToString("\n")
        }
    }

    fun pushToQuestList() {
        val d = doc as? Doc.Quest ?: return
        work {
            val m = withContext(Dispatchers.IO) {
                d.q.save { }  // writes edits into the arc entries first
                pushToList(d)
            }
            message = m
        }
    }

    // the list the quest was found in, by its current id or the id it had when opened
    private fun listNameFor(d: Doc.Quest): String? =
        quests.firstOrNull { it.id == d.q.data.id && it.list.isNotEmpty() }?.list
            ?: quests.firstOrNull { it.id == d.firstId && it.list.isNotEmpty() }?.list

    // returns a message for the snackbar
    private fun pushToList(d: Doc.Quest): String {
        val name = listNameFor(d) ?: return ctx.getString(R.string.msg_list_none, "q%07d".format(d.q.data.id))
        val copies = prefs.lists.filter { it.first == name }.map { Uri.parse(it.second) }
        if (copies.isEmpty()) return ctx.getString(R.string.msg_list_none, "q%07d".format(d.q.data.id))
        var added = false
        for (uri in copies) {
            val list = Arc.read(read(uri))
            d.q.pushToQuestList(list) { added = true }
            write(uri, list.write())
        }
        // keep the browser in sync with the new name / stars
        val info = QuestInfo(
            d.q.data.id, name, d.q.data.level, d.q.data.map, d.q.data.icon(0),
            List(7) { d.q.text.getOrNull(it)?.entries?.firstOrNull() ?: "" },
            quests.firstOrNull { it.id == d.firstId }?.file ?: d.uri.toString(),
        )
        quests = quests.filter { it.id != d.firstId && it.id != info.id } + info
        runCatching { indexFile.writeText(QuestIndex.save(quests)) }
        return ctx.getString(if (added) R.string.msg_list_added else R.string.msg_list_updated, "$name.arc")
    }

    fun changeAutoUpdate(v: Boolean) {
        prefs.autoUpdate = v
        autoUpdate = v
    }

    // walks a picked folder with one query per directory, DocumentFile does one per file which is way too slow on a full dump
    private fun walkTree(tree: Uri, prune: Boolean = false, visit: (path: String, doc: Uri) -> Unit) {
        val cr = ctx.contentResolver
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val todo = ArrayDeque<Pair<String, String>>()
        todo += DocumentsContract.getTreeDocumentId(tree) to ""
        while (todo.isNotEmpty()) {
            val (id, path) = todo.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
            cr.query(children, cols, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val cid = c.getString(0)
                    val name = c.getString(1) ?: continue
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // a whole romfs is tens of thousands of files, only go where quests and maps live
                        val depth = path.count { it == '/' }
                        if (!prune || depth < 2 || GAME_DIRS.matches(name)) todo += cid to "$path/$name"
                    } else visit("$path/$name", DocumentsContract.buildDocumentUriUsingTree(tree, cid))
                }
            }
        }
    }

    private fun indexQuests(files: Map<Int, String>, lists: List<Triple<String, String, Uri>>): List<QuestInfo> {
        val infos = LinkedHashMap<Int, QuestInfo>()
        for (name in QuestIndex.LISTS) {
            // same list can be in two places in a dump, read one, write all of them later
            val copy = lists.filter { it.first == name }.sortedByDescending { "/loc/" in it.second }.firstOrNull() ?: continue
            for (q in QuestIndex.read(name, Arc.read(read(copy.third)))) infos.putIfAbsent(q.id, q.copy(file = files[q.id] ?: ""))
        }
        for ((id, f) in files) if (id !in infos) infos[id] = QuestInfo(id, "", 0, 0, 0, emptyList(), f)
        prefs.lists = lists.map { it.first to it.third.toString() }
        val all = infos.values.sortedBy { it.id }
        indexFile.writeText(QuestIndex.save(all))
        return all
    }

    // one png per area, english copy if there's one, otherwise whatever language is there
    private fun saveMaps(found: List<Pair<String, () -> ByteArray>>): Int {
        mapDir.mkdirs()
        var n = 0
        val best = found.groupBy { StageMaps.areaOf(it.first)!! }.mapValues { (_, v) -> v.firstOrNull { "_ui_eng" in it.first } ?: v.first() }
        for ((area, pick) in best) {
            val (w, h, px) = runCatching { StageMaps.decode(pick.second()) }.getOrNull() ?: continue
            val bmp = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
            File(mapDir, "m%02d_a%02d.png".format(area.first, area.second)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            n++
        }
        return n
    }

    /** one pick for everything: quests, quest lists and area maps under a romfs (or a folder holding one) */
    fun scanGame(uri: Uri) = work {
        keep(uri)
        val (count, maps) = withContext(Dispatchers.IO) {
            val files = HashMap<Int, String>()
            val lists = ArrayList<Triple<String, String, Uri>>()
            val mapFiles = ArrayList<Pair<String, () -> ByteArray>>()
            walkTree(uri, prune = true) { path, doc ->
                val name = path.substringAfterLast('/')
                QuestIndex.questId(name)?.let { files[it] = doc.toString() }
                QuestIndex.listName(name)?.let { lists += Triple(it, path, doc) }
                if (StageMaps.areaOf(path) != null) mapFiles += path to { read(doc) }
            }
            if (files.isEmpty() && lists.isEmpty()) throw IOException(ctx.getString(R.string.err_no_quests))
            val all = indexQuests(files, lists)
            val m = if (mapFiles.isNotEmpty()) saveMaps(mapFiles) else 0
            all to m
        }
        quests = count
        val name = nameOf(uri)
        prefs.folderName = name
        folderName = name
        if (maps > 0) {
            mapCache.clear()
            mapCount = mapDir.list()?.size ?: 0
            prefs.stageName = name
            stageName = name
        }
        say(R.string.msg_scan_done, count.size, maps)
    }

    /** indexes every quest and quest list under the picked folder */
    fun scanFolder(uri: Uri) = work {
        keep(uri)
        val found = withContext(Dispatchers.IO) {
            val files = HashMap<Int, String>()
            val lists = ArrayList<Triple<String, String, Uri>>()  // name, path, uri
            walkTree(uri) { path, doc ->
                val name = path.substringAfterLast('/')
                QuestIndex.questId(name)?.let { files[it] = doc.toString() }
                QuestIndex.listName(name)?.let { lists += Triple(it, path, doc) }
            }
            if (files.isEmpty() && lists.isEmpty()) throw IOException(ctx.getString(R.string.err_no_quests))
            indexQuests(files, lists)
        }
        quests = found
        val name = nameOf(uri)
        prefs.folderName = name
        folderName = name
        say(R.string.quests_found, found.size)
    }

    fun suggestedName(): String {
        val d = doc ?: return "quest.arc"
        return when (d) {
            is Doc.Quest -> "q%07d.arc".format(d.q.data.id)
            else -> d.name
        }
    }

    // raw export / import of the supply or questplus file
    fun exportRaw(to: Uri, bytes: ByteArray) = work {
        withContext(Dispatchers.IO) { write(to, bytes) }
        say(R.string.msg_saved, nameOf(to))
    }

    fun importEsl(uri: Uri, slot: Int) = work {
        val q = (doc as? Doc.Quest)?.q ?: return@work
        val esl = withContext(Dispatchers.IO) { EmSetList.read(read(uri)) }
        edit {
            esl.packs.forEachIndexed { i, p -> q.esls[slot].packs[i] = p }
        }
    }

    fun importRaw(uri: Uri, into: ByteArray) = work {
        val b = withContext(Dispatchers.IO) { read(uri) }
        if (b.size != into.size || b.s32(0) != into.s32(0)) throw IOException(ctx.getString(R.string.err_wrong_file, into.size, b.size))
        edit { b.copyInto(into) }
    }
}
