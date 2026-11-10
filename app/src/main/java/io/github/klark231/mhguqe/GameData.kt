package io.github.klark231.mhguqe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** one entry in a picker: the value written to the file and what the user sees */
data class Opt(val value: Int, val label: String, val icon: Int = -1)

class SpawnPreset(val map: Int, val area: Int, val index: Int, val spawn: Spawn)

data class Weapon(val name: String, val family: Int, val level: Int)

/** Everything from assets/res, the same files the desktop editor ships. */
class GameData(private val ctx: Context) {
    private fun text(name: String) = ctx.assets.open("res/$name").use { it.readBytes().toString(Charsets.UTF_8) }
    private fun bytes(name: String) = ctx.assets.open("res/$name").use { it.readBytes() }
    private fun strings(name: String): List<String> = JSONArray(text(name)).let { a -> List(a.length()) { a.getString(it) } }
    private fun indexed(name: String) = strings(name).mapIndexed { i, s -> Opt(i, s) }
    private fun named(name: String): List<Opt> = JSONArray(text(name)).let { a ->
        List(a.length()) { a.getJSONObject(it).let { o -> Opt(o.getInt("Value"), o.getString("Name")) } }
    }

    /** game language index (see Lang.codes). Names of items, monsters, skills, maps... follow it. */
    var lang by mutableIntStateOf(0)

    /** assets/res/names/<code>.json, generated from the game's own text tables */
    private class Loc(
        val items: List<String>, val skills: List<String>, val monsters: Map<String, String>, val maps: Map<Int, String>,
        val armor: List<String>, val arts: List<String>, val weaponBases: Map<String, String>, val hyper: String,
        val iconNames: Map<String, String>, val head: String, val tail: String,
    )

    private val locs = HashMap<Int, Loc?>()
    private fun loc(l: Int): Loc? = locs.getOrPut(l) {
        runCatching {
            val o = JSONObject(text("names/${Lang.codes[l]}.json"))
            fun list(k: String) = o.getJSONArray(k).let { a -> List(a.length()) { a.getString(it) } }
            fun map(k: String) = o.getJSONObject(k).let { m -> m.keys().asSequence().associateWith { m.getString(it) } }
            Loc(list("items"), list("skills"), map("monsters"), map("maps").mapKeys { it.key.toInt() },
                list("armor"), list("arts"), map("weaponBases"), o.optString("hyper", "Hyper"),
                runCatching { map("iconNames") }.getOrDefault(emptyMap()), o.optString("head", "Head"), o.optString("tail", "Tail"))
        }.getOrNull()
    }
    private val cur get() = loc(lang)

    /** everything that carries a name, built once per language */
    private inner class View(val l: Loc?) {
        val itemNames = if (l == null) rawItemNames else rawItemNames.mapIndexed { i, s -> l.items.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: s }
        val items = itemNames.mapIndexed { i, s -> Opt(i, s) }
        val decorations = items.filter { it.value == 0 || rawItemNames[it.value].contains("Jwl", ignoreCase = true) }
        val skills = rawSkillNames.mapIndexed { i, s -> Opt(i, l?.skills?.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: s) }
        val maps = rawMaps.map { Opt(it.value, l?.maps?.get(it.value) ?: it.label) }.sortedBy { it.label }
        fun mon(en: String) = l?.monsters?.get(en) ?: en
        val monsters = rawMonsters.map { Opt(it.value, mon(it.label)) }
        val smallMonsters = rawSmall.map { Opt(it.value, mon(it.label)) }
        val smallAndLarge = smallMonsters + monsters.filter { it.value != 0 }.map { it.copy(label = it.label + " ★") }
        private val chinese = Lang.codes[lang].startsWith("ch")
        // the icon list uses its own spellings for a few monsters
        fun iconLabel(s: String): String {
            l?.iconNames?.get(s)?.let { return it }
            if (s == "Nakarkos Head" || s == "Nakarkos Tail") {
                val n = mon("Nakarkos")
                val w = if (l == null) s.removePrefix("Nakarkos ") else if (s.endsWith("Head")) l.head else l.tail
                return if (chinese) n + w else "$n $w"
            }
            if (s == "Hyper None") return l?.hyper ?: s
            if (s.startsWith("Hyper ")) {
                val b = mon(iconAlias[s.removePrefix("Hyper ")] ?: s.removePrefix("Hyper "))
                val h = l?.hyper ?: "Hyper"
                return if (chinese) h + b else "$h $b"
            }
            return mon(iconAlias[s] ?: s)
        }
        val icons = rawIconNames.mapIndexed { i, s -> Opt(i, iconLabel(s), if (i == 0) -1 else i - 1) }
    }

    private val iconAlias = mapOf(
        "Ahtal Ka Mecha" to "Ahtal-Neset", "Ahtal Ka" to "Ahtal-Ka", "Yiak Kut Ku" to "Yian Kut-Ku",
        "Yian Kut Ku" to "Yian Kut-Ku", "Lao Shan Lung" to "Lao-Shan Lung",
    )
    private val views = HashMap<Int, View>()
    private val view: View get() = views.getOrPut(lang) { View(cur) }

    val questTypes = indexed("quest_type.json")
    val subTypes = indexed("quest_subtype.json")
    val levels = named("quest_level.json")
    val enemyLevels = named("em_level.json")
    private val rawMaps = named("map_names.json")
    val maps get() = view.maps
    val startTypes = indexed("quest_start_type.json")
    val bgm = indexed("quest_bgm.json")
    val requirements = indexed("quest_requirement.json")
    val clearTypes = indexed("quest_clear_type.json")
    val objectives = indexed("quest_objective.json")
    val carveLevels = indexed("quest_carve_level.json")
    val gatherLevels = indexed("quest_gather_level.json")
    val fishLevels = indexed("quest_fishing_level.json")

    private val rawItemNames = strings("item_names.json")
    private val rawSkillNames = strings("skill_names.json")
    val itemNames get() = view.itemNames
    val items get() = view.items
    // decorations are the "... Jwl" items (matched on the english name)
    val decorations get() = view.decorations

    private val rawIconNames = strings("icon_names.json")
    val icons get() = view.icons

    private val rawMonsters: List<Opt>
    val monsters get() = view.monsters
    val baseHp = HashMap<Int, Int>()

    init {
        val o = JSONObject(text("em_names.json"))
        val list = ArrayList<Opt>()
        for (k in o.keys()) {
            val m = o.getJSONObject(k)
            list += Opt(m.getInt("Id"), k)
            baseHp[m.getInt("Id")] = m.getInt("BaseHp")
        }
        rawMonsters = list
    }

    private val rawSmall: List<Opt> = JSONObject(text("ems_names.json")).let { o ->
        o.keys().asSequence().map { Opt(o.getInt(it), it) }.sortedBy { it.value }.toList()
    }
    val smallMonsters get() = view.smallMonsters

    // large monsters offered in the small monster picker. No game file does this, see ems_large_hint
    val smallAndLarge get() = view.smallAndLarge

    val stats = StatTable.read(bytes("em_nando_tbl.nan"))
    private fun pct(f: Float) = "%.0f%%".format(f * 100)
    val hpRows = stats.rows.mapIndexed { i, r -> Opt(i, "$i · " + pct(r[0])) }
    val atkRows = stats.rows.mapIndexed { i, r -> Opt(i, "$i · " + pct(r[1])) }
    val otherRows = stats.rows.mapIndexed { i, r -> Opt(i, "$i · " + (3..7).joinToString(" / ") { pct(r[it]) }) }

    val spawnPresets: List<SpawnPreset> = buildList {
        val maps = JSONArray(text("spawns.json"))
        for (i in 0 until maps.length()) {
            val m = maps.getJSONObject(i)
            val mapId = m.getInt("map_id")
            if (mapId == 0) continue
            val areas = m.getJSONArray("areas")
            for (j in 0 until areas.length()) {
                val a = areas.getJSONObject(j)
                val area = a.getInt("area")
                val sp = a.getJSONArray("spawns")
                for (k in 0 until sp.length()) {
                    val s = sp.getJSONObject(k)
                    add(SpawnPreset(mapId, area, k + 1, Spawn(
                        s.getInt("round_no"), area, s.getDouble("angle").toFloat(),
                        s.getDouble("x").toFloat(), s.getDouble("y").toFloat(), s.getDouble("z").toFloat(),
                    )))
                }
            }
        }
    }

    // every spot the game's own quests put a monster, per map and area, and the
    // fitted map image transform (scale, offset x, offset y) for each area
    private val placed: JSONObject by lazy { JSONObject(text("positions.json")) }

    fun positions(map: Int, area: Int): List<FloatArray> {
        val a = placed.getJSONObject("points").optJSONArray("${map}_$area") ?: return emptyList()
        return List(a.length()) { i -> a.getJSONArray(i).let { p -> FloatArray(3) { p.getDouble(it).toFloat() } } }
    }

    fun calibration(map: Int, area: Int): FloatArray? =
        placed.getJSONObject("calib").optJSONArray("${map}_$area")?.let { a -> FloatArray(3) { a.getDouble(it).toFloat() } }

    fun knownAreas(map: Int): List<Int> {
        val fromQuests = placed.getJSONObject("points").keys().asSequence()
            .filter { it.startsWith("${map}_") }.map { it.substringAfter('_').toInt() }
        return (fromQuests + spawnPresets.filter { it.map == map }.map { it.area }).filter { it > 0 }.toSortedSet().toList()
    }

    fun mapName(id: Int) = maps.firstOrNull { it.value == id }?.label ?: "#$id"
    fun itemName(id: Int) = itemNames.getOrNull(id) ?: "#$id"
    fun monsterName(id: Int) = monsters.firstOrNull { it.value == id }?.label ?: "#$id"
    fun smallName(id: Int) = smallMonsters.firstOrNull { it.value == id }?.label
        ?: monsters.firstOrNull { it.value == id && id != 0 }?.let { it.label + " ★" } ?: "#$id"

    // arena stuff is big, only load it when someone opens an arena file
    val arena by lazy { ArenaData() }

    inner class ArenaData {
        val weaponTypes = listOf(
            7 to "Great Sword", 14 to "Long Sword", 8 to "Sword and Shield", 18 to "Dual Blades",
            9 to "Hammer", 19 to "Hunting Horn", 10 to "Lance", 16 to "Gunlance", 15 to "Switch Axe",
            21 to "Charge Blade", 20 to "Insect Glaive", 13 to "Light Bowgun", 11 to "Heavy Bowgun", 17 to "Bow",
        ).map { Opt(it.first, it.second) }
        val styles = listOf("Guild", "Striker", "Aerial", "Adept", "Alchemy", "Valor").mapIndexed { i, s -> Opt(i, s) }
        val skills get() = view.skills

        private val rawArts: List<Opt> = JSONObject(text("hunter_arts.json")).let { o ->
            o.keys().asSequence().map { Opt(o.getInt(it), it) }.sortedBy { it.value }.toList()
        }
        private val artsCache = HashMap<Int, List<Opt>>()
        val arts: List<Opt> get() = artsCache.getOrPut(lang) {
            val l = cur
            rawArts.map { Opt(it.value, if (it.value == 0) it.label else l?.arts?.getOrNull(it.value)?.takeIf { s -> s.isNotEmpty() } ?: it.label) }
        }

        private val rawWeapons: Map<Int, List<Weapon>>
        private val weaponCache = HashMap<Int, Map<Int, List<Weapon>>>()
        // "<weapon line> <n>" in english, the line is translated and the number kept
        val weapons: Map<Int, List<Weapon>> get() = weaponCache.getOrPut(lang) {
            val b = cur?.weaponBases ?: return@getOrPut rawWeapons
            rawWeapons.mapValues { (_, list) ->
                list.map { w ->
                    val base = w.name.substringBeforeLast(' ', "")
                    val n = w.name.substringAfterLast(' ', "")
                    val t = b[base]
                    if (t != null && n.toIntOrNull() != null) w.copy(name = "$t $n") else w
                }
            }
        }
        init {
            val a = JSONArray(text("weapons.json"))
            val byName = HashMap<String, List<Weapon>>()
            for (i in 0 until a.length()) {
                val t = a.getJSONObject(i)
                val w = t.getJSONArray("Weapons")
                byName[t.getString("Type")] = listOf(Weapon("None", 0, 0)) + List(w.length()) {
                    w.getJSONObject(it).let { x -> Weapon(x.getString("Name"), x.getInt("Family"), x.getInt("Level")) }
                }
            }
            rawWeapons = weaponTypes.associate { it.value to (byName[it.label] ?: listOf(Weapon("None", 0, 0))) }
        }

        // armor_series_data.gmd has 10 strings per series: 5 names then 5 descriptions
        private val rawArmor: List<List<Opt>>
        private val armorCache = HashMap<Int, List<List<Opt>>>()
        val armor: List<List<Opt>> get() = armorCache.getOrPut(lang) {
            val l = cur ?: return@getOrPut rawArmor
            rawArmor.mapIndexed { p, opts -> opts.map { it.copy(label = l.armor.getOrNull(it.value * 10 + p)?.takeIf { s -> s.isNotEmpty() } ?: it.label) } }
        }
        val maxLevel = HashMap<Int, Int>()
        init {
            val names = Gmd.read(bytes("armor_series_data.gmd")).entries
            val series = JSONArray(text("armor_series_data.json"))
            val parts = List(5) { ArrayList<Opt>() }
            for (i in 0 until series.length()) {
                val s = series.getJSONObject(i)
                val id = s.getInt("Id")
                maxLevel[id] = s.getInt("MaxLevel")
                val pieces = s.getJSONArray("Pieces")
                for (p in 0 until 5) {
                    if (pieces.getInt(p) != 0) parts[p] += Opt(id, names.getOrElse(id * 10 + p) { "#$id" })
                }
            }
            rawArmor = parts
        }
    }

    // monster icon atlas: 70px cells, 2px gaps, 7 columns. second sheet only has 7 rows
    private val sheets = arrayOfNulls<Bitmap>(2)
    private val iconCache = HashMap<Int, ImageBitmap>()

    fun icon(index: Int): ImageBitmap? {
        if (index < 0) return null
        iconCache[index]?.let { return it }
        // the atlas has no cells for the "Hyper ..." entries, the game draws the monster on yellow spikes
        val name = rawIconNames.getOrNull(index + 1)
        val img = if (name != null && name.startsWith("Hyper ")) hyperIcon(name.removePrefix("Hyper ")) else cell(index)?.asImageBitmap()
        return img?.also { iconCache[index] = it }
    }

    private fun cell(index: Int): Bitmap? {
        if (index < 0 || index >= 7 * 14 + 7 * 7) return null
        val sheet = if (index < 98) 0 else 1
        val n = if (index < 98) index else index - 98
        val bmp = sheets[sheet] ?: ctx.assets.open("res/cmn_micon0$sheet.png").use { BitmapFactory.decodeStream(it) }
            .also { sheets[sheet] = it }
        val x = (n % 7) * 72 + 1
        val y = (n / 7) * 72 + 1
        if (x + 70 > bmp.width || y + 70 > bmp.height) return null
        return Bitmap.createBitmap(bmp, x, y, 70, 70)
    }

    private fun hyperIcon(base: String): ImageBitmap {
        val value = rawIconNames.indexOf(if (base == "Yian Kut Ku") "Yiak Kut Ku" else base)
        val out = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFBAB12A.toInt()
            style = android.graphics.Paint.Style.FILL
        }
        // 8 spikes: one at the middle of every side, one in every corner, drawn mirrored from the top left
        fun tri(path: android.graphics.Path, sx: Float, sy: Float, swap: Boolean, pts: List<Pair<Float, Float>>) {
            pts.forEachIndexed { i, (a, b) ->
                val (u, v) = if (swap) b to a else a to b
                val px = if (sx < 0) 70f - u else u
                val py = if (sy < 0) 70f - v else v
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }
        val path = android.graphics.Path()
        for (sx in listOf(1f, -1f)) for (sy in listOf(1f, -1f)) {
            tri(path, sx, sy, false, listOf(0f to 0f, 24f to 3f, 3f to 24f))  // corner
        }
        tri(path, 1f, 1f, false, listOf(35f to 0f, 27f to 14f, 43f to 14f))  // top
        tri(path, 1f, -1f, false, listOf(35f to 0f, 27f to 14f, 43f to 14f)) // bottom
        tri(path, 1f, 1f, true, listOf(35f to 0f, 27f to 14f, 43f to 14f))   // left
        tri(path, -1f, 1f, true, listOf(35f to 0f, 27f to 14f, 43f to 14f))  // right
        c.drawPath(path, p)
        if (value > 0) cell(value - 1)?.let { c.drawBitmap(it, 0f, 0f, null) }
        return out.asImageBitmap()
    }
}
