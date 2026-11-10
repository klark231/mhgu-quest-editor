package io.github.klark231.mhguqe

import java.io.IOException
import kotlin.random.Random

/**
 * A loaded qXXXXXXX.arc. Holds the decoded pieces we edit and writes them
 * back into the arc on save.
 */
class QuestArc(val arc: Arc) {
    val dataEntry = arc.firstByExt(".ext") ?: throw IOException("no quest data (.ext) in this arc")
    val linkEntry = arc.firstByExt(".qdl") ?: throw IOException("no quest link (.qdl) in this arc")

    val data = QuestData(dataEntry.bytes())
    val link = QuestLink(linkEntry.bytes())

    // null = language missing from the arc
    val text: List<Gmd?> = List(7) { lang -> gmdEntry(lang)?.let { Gmd.read(it.bytes()) } }
    val rems: List<Rem> = QuestLink.REM.map { slot -> entryFor(slot)?.let { Rem(it.bytes()) } ?: Rem.empty() }
    val esls: List<EmSetList> = QuestLink.ESL.map { slot -> entryFor(slot)?.let { EmSetList.read(it.bytes()) } ?: EmSetList() }
    val spawns: MutableList<Spawn> = QuestLink.BOSS.map { slot -> entryFor(slot)?.let { Spawn.read(it.bytes()) } ?: Spawn() }.toMutableList()
    val supplyEntry = entryFor(QuestLink.SUPP)
    val supply: Supply? = supplyEntry?.let { Supply(it.bytes()) }
    val plusEntry = entryFor(QuestLink.PLUS)
    val plus: QuestPlus? = plusEntry?.let { QuestPlus(it.bytes()) }

    private fun gmdEntry(lang: Int): ArcEntry? {
        val f = data.infoFile(lang)
        if (f.isEmpty()) return null
        val code = Lang.codes[lang]
        return arc.find("$code\\quest\\questData\\questData_$f") ?: arc.find("$code\\quest\\questData\\$f")
    }

    /** entry a link slot points at, null for empty placeholders */
    fun entryFor(slot: Int): ArcEntry? {
        if (link.isEmpty(slot)) return null
        return link.path(slot)?.let { arc.find(it) }
    }

    fun save(log: (String) -> Unit = {}): ByteArray {
        val id = data.id

        for (lang in 0 until 7) {
            val gmd = text[lang] ?: continue
            val e = gmdEntry(lang) ?: continue
            val name = "%07d_%s".format(id, Lang.codes[lang])
            data.setInfoFile(lang, name)
            e.path = "${Lang.codes[lang]}\\quest\\questData\\questData_$name"
            val b = gmd.write()
            // gmd.write() stamps a new time, so compare without the header time
            val old = e.bytes()
            if (old.size != b.size || old.indices.any { it !in 12..19 && old[it] != b[it] }) e.set(b)
        }

        dataEntry.path = "loc\\quest\\questData\\questData_%07d".format(id)
        if (!dataEntry.bytes().contentEquals(data.raw)) dataEntry.set(data.raw)

        QuestLink.REM.forEachIndexed { i, slot ->
            val rem = rems[i]
            // vanilla rem ids stop around 730000
            write(slot, rem.rewards().isEmpty(), rem.raw, "rRem", log) { freshName(slot) }
        }

        QuestLink.ESL.forEachIndexed { i, slot ->
            val esl = esls[i]
            write(slot, esl.isEmpty, esl.write(), "rEmSetList", log) { freshName(slot) }
        }

        QuestLink.BOSS.forEachIndexed { i, slot ->
            val sp = spawns[i]
            write(slot, sp.isEmpty, sp.write(), "rSetEmMain", log) { freshName(slot) }
        }

        supply?.let { if (!supplyEntry!!.bytes().contentEquals(it.raw)) supplyEntry.set(it.raw) }
        plus?.let { if (!plusEntry!!.bytes().contentEquals(it.raw)) plusEntry.set(it.raw) }

        linkEntry.path = "loc\\quest\\questLink\\questLink_%07d".format(id)
        if (!linkEntry.bytes().contentEquals(link.raw)) linkEntry.set(link.raw)

        return arc.write(sortedEntries())
    }

    // (arc path, link name) with a random id, may already be taken
    private fun freshName(slot: Int): Pair<String, String> {
        val map = data.map.coerceAtLeast(0)
        return when (slot) {
            in QuestLink.REM -> QuestLink.remName(Random.nextInt(750000, 1000000)).let { "quest\\rem\\$it" to it }
            in QuestLink.ESL -> QuestLink.eslName(map, Random.nextInt(100, 1000)).let { "quest\\zako\\emSetList\\$it" to it }
            else -> QuestLink.semName(map, data.monsters[slot].id and 0xFF, Random.nextInt(10, 100))
                .let { "quest\\boss\\setEmMain\\$it" to it }
        }
    }

    // unchanged -> leave the entry alone (some game files are linked but empty)
    // emptied -> point the slot at the shared blank file
    // existing entry -> overwrite it
    // blank slot that got content -> add a new file with a free random name
    private fun write(
        slot: Int, empty: Boolean, bytes: ByteArray, type: String, log: (String) -> Unit,
        newName: () -> Pair<String, String>,
    ) {
        val current = entryFor(slot)
        if (current != null && current.bytes().contentEquals(bytes)) return
        if (empty) {
            if (!link.isEmpty(slot)) log("cleared ${link.file(slot)}")
            link.clear(slot)
            return
        }
        if (current != null) {
            current.set(bytes)
            return
        }
        repeat(500) {
            val (path, name) = newName()
            if (arc.find(path) != null) return@repeat
            arc.add(path, type, bytes)
            link.set(slot, type, name)
            log("added $path")
            return
        }
        throw IOException("couldn't find a free file name for $type")
    }

    // order the game expects: sem, esl, rem, sup, qdp, qdl, gmd, ext
    // anything else is kept at the end instead of being dropped
    private fun sortedEntries(): List<ArcEntry> {
        val order = listOf(".sem", ".esl", ".rem", ".sup", ".qdp", ".qdl", ".gmd", ".ext")
        val known = order.flatMap { ext -> arc.entries.filter { it.ext == ext } }
        return known + arc.entries.filter { it.ext !in order }
    }

    /** copies quest data and text into a quest list arc (gq.arc etc). */
    fun pushToQuestList(list: Arc, log: (String) -> Unit) {
        val id = data.id
        val qPath = "loc\\quest\\questData\\questData_%07d".format(id)
        val q = list.find(qPath)
        if (q != null) q.copyStored(dataEntry) else {
            list.entries += ArcEntry(qPath, dataEntry.typeHash, dataEntry.data.copyOf(), dataEntry.realSize, dataEntry.quality)
            log("added quest $id to the quest list")
        }
        for (lang in 0 until 7) {
            val src = gmdEntry(lang) ?: continue
            val dst = list.find(src.path)
            if (dst != null) dst.copyStored(src) else {
                list.entries += ArcEntry(src.path, src.typeHash, src.data.copyOf(), src.realSize, src.quality)
            }
        }
    }

    companion object {
        fun isQuestArcName(name: String): Boolean {
            val stem = name.substringBeforeLast('.')
            return stem.length == 8 && stem[0] == 'q' && stem.drop(1).all { it.isDigit() }
        }
    }
}

/**
 * Extra large monsters: copies of a large monster in a small monster wave, so
 * more than the 5 slots can be out in one area. Same setup as a working MHXX
 * custom quest with 3 Fatalis: extras in wave 1 of the area, their stats in the
 * empty large monster slot after the real ones, and the hunt count covering all.
 */
object Extras {
    fun isLarge(id: Int) = id in 1 until 0x1000

    /** how many of this monster the quest has, (in large slots, in small monster waves) */
    fun count(q: QuestArc, id: Int): Pair<Int, Int> =
        q.data.monsters.count { it.id == id } to q.esls.sumOf { w -> w.packs.sumOf { p -> p.count { it.monster == id } } }

    /** spots: known positions in that area (x, y, z), used in turn so the copies don't stack up */
    fun add(q: QuestArc, id: Int, area: Int, copies: Int, spots: List<FloatArray>, rnd: kotlin.random.Random = kotlin.random.Random) {
        require(area in 1..EmSetList.SLOTS) { "area $area" }
        val list = q.esls[0].packs[area - 1]
        val base = q.spawns.firstOrNull { it.area == area && !it.isEmpty }
        val taken = list.map { it.pos(0) to it.pos(2) }.toMutableSet()
        val free = spots.filter { (it[0] to it[2]) !in taken }.ifEmpty { spots }
        repeat(copies) { n ->
            val e = Ems()
            // byte values the game's own entries use most
            e.monster = id
            e.setUnk1(0, 1); e.setUnk1(1, 0xCD)
            e.condition = 255
            e.area = area
            e.setUnk2(0, 1); e.setUnk2(1, 0)
            e.setUnk3(0, rnd.nextInt(1, 0x10000)); e.setUnk3(1, 0)
            val p = free.getOrNull(n % free.size.coerceAtLeast(1))
            // more copies than spots: same spot, nudged over a bit each round
            val nudge = 250f * (n / free.size.coerceAtLeast(1))
            when {
                p != null -> { e.setPos(0, p[0] + nudge); e.setPos(1, p[1]); e.setPos(2, p[2]) }
                base != null -> { e.setPos(0, base.x + 300f * (n + 1)); e.setPos(1, base.y); e.setPos(2, base.z) }
            }
            e.angle = rnd.nextInt(0, 360).toFloat()
            list += e
        }
        giveStats(q, id)
        recount(q, id)
    }

    fun remove(q: QuestArc, id: Int) {
        q.esls.forEach { w -> w.packs.forEach { p -> p.removeAll { it.monster == id } } }
        recount(q, id)
    }

    // stats from the real slot of that monster into the first empty slot after the used ones
    private fun giveStats(q: QuestArc, id: Int) {
        val ms = q.data.monsters
        val src = ms.firstOrNull { it.id == id } ?: ms.firstOrNull { it.id != 0 } ?: return
        val dst = ms.drop(ms.indexOfLast { it.id != 0 } + 1).firstOrNull() ?: return
        dst.hpTable = src.hpTable; dst.atkTable = src.atkTable; dst.otherTable = src.otherTable
        dst.size = src.size; dst.sizeTable = src.sizeTable; dst.staminaTable = src.staminaTable
        dst.difficulty = src.difficulty; dst.restore = src.restore
    }

    // hunt/capture/slay objectives on this monster cover the slots and the extras
    private fun recount(q: QuestArc, id: Int) {
        val (slots, waves) = count(q, id)
        val total = slots + waves
        val d = q.data
        val hit = (0 until 2).filter { d.condParam(it) in 1..4 && d.condValue(it) == id }
        if (hit.isNotEmpty()) hit.forEach { d.setCondCount(it, total.coerceAtLeast(1)) }
        else if (waves > 0 && d.condParam(0) == 0) {
            d.setCondParam(0, 1); d.setCondValue(0, id); d.setCondCount(0, total)
        }
    }
}
