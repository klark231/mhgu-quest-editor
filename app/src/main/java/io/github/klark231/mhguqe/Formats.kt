package io.github.klark231.mhguqe

import java.io.ByteArrayOutputStream
import java.io.IOException

// Everything here keeps the original bytes around and edits them in place,
// so fields we don't show (or don't know about) survive a save untouched.

private fun header(b: ByteArray, magic: Int, what: String) {
    if (b.size < 8 || b.s32(0) != magic) throw IOException("$what: bad header %08X".format(if (b.size >= 4) b.s32(0) else 0))
}

private fun need(b: ByteArray, size: Int, what: String) {
    if (b.size < size) throw IOException("$what: file too short (${b.size} < $size)")
}

object Lang {
    val codes = listOf("eng", "fre", "ger", "ita", "spa", "chT", "chS")
    val names = listOf("English", "Français", "Deutsch", "Italiano", "Español", "繁體中文", "简体中文")
}

/**
 * rQuestData (.ext / .mib). 8 byte header then one packed record, 361 bytes
 * with all 7 languages. Training quests only carry 5 (321 bytes) and MHXX on
 * 3DS just one (241), the fields after the language table move with it.
 */
class QuestData(val raw: ByteArray) {
    val langs: Int

    init {
        header(raw, MAGIC, "quest data")
        need(raw, 8 + 215 + 20 + 6, "quest data")
        langs = ((raw.size - 8 - 215 - 6) / 20).coerceIn(1, 7)
    }

    private fun i8(o: Int) = raw.u8(8 + o)
    private fun s8(o: Int) = raw.s8(8 + o)
    private fun i16(o: Int) = raw.u16(8 + o)
    private fun i32(o: Int) = raw.s32(8 + o)
    private fun w8(o: Int, v: Int) = raw.put8(8 + o, v)
    private fun w16(o: Int, v: Int) = raw.put16(8 + o, v)
    private fun w32(o: Int, v: Int) = raw.put32(8 + o, v)

    var index get() = i32(0); set(v) = w32(0, v)
    var id get() = i32(4); set(v) = w32(4, v)
    var type get() = i8(8); set(v) = w8(8, v)
    var subType get() = i8(9); set(v) = w8(9, v)
    var level get() = i8(10); set(v) = w8(10, v)
    var enemyLevel get() = i8(11); set(v) = w8(11, v)
    var map get() = s8(12); set(v) = w8(12, v)
    var startType get() = i8(13); set(v) = w8(13, v)
    var time get() = i8(14); set(v) = w8(14, v)
    var faints get() = i8(15); set(v) = w8(15, v)
    var arenaEquipId get() = i8(16); set(v) = w8(16, v)
    var bgm get() = i8(17); set(v) = w8(17, v)
    var req1 get() = i8(18); set(v) = w8(18, v)
    var req2 get() = i8(19); set(v) = w8(19, v)
    var comboReq get() = i8(20); set(v) = w8(20, v)
    var clearType get() = i8(21); set(v) = w8(21, v)
    var gekitaiHp get() = i8(22); set(v) = w8(22, v)

    // 0, 1 = main objectives, 2 = subquest
    fun condOff(i: Int) = 23 + i * 5
    fun condParam(i: Int) = i8(condOff(i))
    fun setCondParam(i: Int, v: Int) = w8(condOff(i), v)
    fun condValue(i: Int) = i16(condOff(i) + 1)
    fun setCondValue(i: Int, v: Int) = w16(condOff(i) + 1, v)
    fun condCount(i: Int) = i16(condOff(i) + 3)
    fun setCondCount(i: Int, v: Int) = w16(condOff(i) + 3, v)

    var carveLevel get() = i8(38); set(v) = w8(38, v)
    var gatherLevel get() = i8(39); set(v) = w8(39, v)
    var fishLevel get() = i8(40); set(v) = w8(40, v)
    var fee get() = i32(41); set(v) = w32(41, v)
    var villagePoints get() = i32(45); set(v) = w32(45, v)
    var reward get() = i32(49); set(v) = w32(49, v)
    var subReward get() = i32(53); set(v) = w32(53, v)
    var clearVillagePoints get() = i32(57); set(v) = w32(57, v)
    var failVillagePoints get() = i32(61); set(v) = w32(61, v)
    var subVillagePoints get() = i32(65); set(v) = w32(65, v)
    var hrp get() = i32(69); set(v) = w32(69, v)
    var subHrp get() = i32(73); set(v) = w32(73, v)
    fun remAddFrame(i: Int) = s8(77 + i)
    fun setRemAddFrame(i: Int, v: Int) = w8(77 + i, v)
    var remAddLotMax get() = s8(79); set(v) = w8(79, v)

    fun suppLabel(i: Int) = s8(80 + i * 6)
    fun setSuppLabel(i: Int, v: Int) = w8(80 + i * 6, v)
    fun suppType(i: Int) = s8(81 + i * 6)
    fun setSuppType(i: Int, v: Int) = w8(81 + i * 6, v)
    fun suppTarget(i: Int) = i16(82 + i * 6)
    fun setSuppTarget(i: Int, v: Int) = w16(82 + i * 6, v)
    fun suppCount(i: Int) = i16(84 + i * 6)
    fun setSuppCount(i: Int, v: Int) = w16(84 + i * 6, v)

    val monsters = List(5) { Monster(92 + it * 13) }

    inner class Monster(private val o: Int) {
        var id get() = i16(o); set(v) = w16(o, v)
        var subType get() = i8(o + 2); set(v) = w8(o + 2, v)
        var aura get() = s8(o + 3); set(v) = w8(o + 3, v)
        var restore get() = i8(o + 4); set(v) = w8(o + 4, v)
        var hpTable get() = i8(o + 5); set(v) = w8(o + 5, v)
        var atkTable get() = i8(o + 6); set(v) = w8(o + 6, v)
        var otherTable get() = i8(o + 7); set(v) = w8(o + 7, v)
        var difficulty get() = i8(o + 8); set(v) = w8(o + 8, v)
        var size get() = i16(o + 9); set(v) = w16(o + 9, v)
        var sizeTable get() = i8(o + 11); set(v) = w8(o + 11, v)
        var staminaTable get() = i8(o + 12); set(v) = w8(o + 12, v)
    }

    var smallHp get() = i8(157); set(v) = w8(157, v)
    var smallAtk get() = i8(158); set(v) = w8(158, v)
    var smallOther get() = i8(159); set(v) = w8(159, v)

    // enemy sets for monster 2 and 3
    fun setType(i: Int) = s8(160 + i * 5)
    fun setSetType(i: Int, v: Int) = w8(160 + i * 5, v)
    fun setTarget(i: Int) = i16(161 + i * 5)
    fun setSetTarget(i: Int, v: Int) = w16(161 + i * 5, v)
    fun setCount(i: Int) = i16(163 + i * 5)
    fun setSetCount(i: Int, v: Int) = w16(163 + i * 5, v)

    var bossRush get() = s8(170); set(v) = w8(170, v)

    fun spawnType(i: Int) = s8(171 + i * 5)
    fun setSpawnType(i: Int, v: Int) = w8(171 + i * 5, v)
    fun spawnTarget(i: Int) = i16(172 + i * 5)
    fun setSpawnTarget(i: Int, v: Int) = w16(172 + i * 5, v)
    fun spawnCount(i: Int) = i16(174 + i * 5)
    fun setSpawnCount(i: Int, v: Int) = w16(174 + i * 5, v)

    var strayRand get() = i8(196); set(v) = w8(196, v)
    var strayStart get() = i8(197); set(v) = w8(197, v)
    var strayStartRand get() = i8(198); set(v) = w8(198, v)
    fun strayLimit(i: Int) = s8(199 + i)
    fun setStrayLimit(i: Int, v: Int) = w8(199 + i, v)
    fun strayRand345(i: Int) = i8(202 + i)
    fun setStrayRand345(i: Int, v: Int) = w8(202 + i, v)
    var extraTickets get() = i8(205); set(v) = w8(205, v)
    fun icon(i: Int) = i8(206 + i)
    fun setIcon(i: Int, v: Int) = w8(206 + i, v)
    var progNum get() = i32(211); set(v) = w32(211, v)

    fun infoFile(lang: Int) = if (lang < langs) raw.cstr(8 + 219 + lang * 20, 16) else ""
    fun setInfoFile(lang: Int, s: String) { if (lang < langs) raw.putCstr(8 + 219 + lang * 20, 16, s) }

    private val tail get() = 215 + langs * 20
    var villagePointsG get() = i32(tail); set(v) = w32(tail, v)
    var flags get() = i16(tail + 4); set(v) = w16(tail + 4, v)

    fun copy() = QuestData(raw.copyOf())

    companion object {
        const val MAGIC = 0x434B0000  // 203.0f
        const val SIZE = 361
    }
}

/**
 * GMD text. Strings are the last StringBufferSize bytes, everything before
 * them (header, package name, any label tables) is kept as is.
 */
class Gmd(private val prefix: ByteArray, val entries: MutableList<String>) {

    fun write(): ByteArray {
        val body = ByteArrayOutputStream()
        for (s in entries) {
            body.write(s.toByteArray(Charsets.UTF_8))
            body.write(0)
        }
        val strings = body.toByteArray()
        val head = prefix.copyOf()
        head.put32(4 + 20, entries.size)
        head.put32(4 + 28, strings.size)
        val t = System.currentTimeMillis() / 1000
        head.put32(4 + 8, t.toInt())
        head.put32(4 + 12, (t ushr 32).toInt())
        return head + strings
    }

    companion object {
        const val MAGIC = 0x444D47

        fun read(b: ByteArray): Gmd {
            header(b, MAGIC, "GMD")
            need(b, 40, "GMD")
            val count = b.s32(4 + 20)
            val bufSize = b.s32(4 + 28)
            if (bufSize < 0 || bufSize > b.size - 40) throw IOException("GMD: bad string buffer size")
            val start = b.size - bufSize
            val list = ArrayList<String>(count)
            var p = start
            repeat(count) {
                var e = p
                while (e < b.size && b[e] != 0.toByte()) e++
                list += String(b, p, e - p, Charsets.UTF_8)
                p = e + 1
            }
            return Gmd(b.copyOfRange(0, start), list)
        }
    }
}

/** Reward list (.rem): 8 flag pairs and up to 40 items, ended by {0, 0, 255}. */
class Rem(val raw: ByteArray) {
    init {
        header(raw, VERSION, "rem")
        need(raw, 8 + 176, "rem")
    }

    data class Reward(val item: Int, val amount: Int, val weight: Int)

    fun flag(i: Int) = raw.u8(8 + i * 2)
    fun setFlag(i: Int, v: Int) = raw.put8(8 + i * 2, v)
    fun flagValue(i: Int) = raw.u8(9 + i * 2)
    fun setFlagValue(i: Int, v: Int) = raw.put8(9 + i * 2, v)

    fun rewards(): List<Reward> {
        val out = ArrayList<Reward>()
        for (i in 0 until MAX) {
            val o = 24 + i * 4
            val r = Reward(raw.u16(o), raw.u8(o + 2), raw.u8(o + 3))
            if (r.item == 0 && r.amount == 0 && (r.weight == 255 || (i == 0 && r.weight == 0))) break
            out += r
        }
        return out
    }

    fun setRewards(list: List<Reward>) {
        raw.fill(0, 24, 24 + MAX * 4)
        list.take(MAX).forEachIndexed { i, r ->
            val o = 24 + i * 4
            raw.put16(o, r.item); raw.put8(o + 2, r.amount); raw.put8(o + 3, r.weight)
        }
        if (list.isNotEmpty() && list.size < MAX) raw.put8(24 + list.size * 4 + 3, 255)
    }

    companion object {
        const val VERSION = 0x3F800000
        const val MAX = 40
        fun empty() = Rem(ByteArray(8 + 176).also { it.put32(0, VERSION); it.put32(4, 1) })
    }
}

/** Quest link (.qdl): names of the sem/esl/rem/sup/qdp files a quest uses. */
class QuestLink(val raw: ByteArray) {
    init {
        header(raw, VERSION, "quest link")
        need(raw, 8 + COUNT * 20, "quest link")
    }

    fun hash(slot: Int) = raw.s32(8 + slot * 20)
    fun file(slot: Int) = raw.cstr(12 + slot * 20, 16)

    fun set(slot: Int, type: String, file: String) {
        raw.put32(8 + slot * 20, typeHash(type))
        raw.putCstr(12 + slot * 20, 16, file)
    }

    fun isEmpty(slot: Int): Boolean = when (hash(slot)) {
        0 -> true
        H_SEM -> file(slot) == EMPTY_SEM
        H_ESL -> file(slot) == EMPTY_ESL
        H_REM -> file(slot) == EMPTY_REM
        else -> false
    }

    fun clear(slot: Int) {
        when (hash(slot)) {
            H_SEM -> raw.putCstr(12 + slot * 20, 16, EMPTY_SEM)
            H_ESL -> raw.putCstr(12 + slot * 20, 16, EMPTY_ESL)
            H_REM -> raw.putCstr(12 + slot * 20, 16, EMPTY_REM)
        }
    }

    /** full arc path for the slot, null when the slot is unused */
    fun path(slot: Int): String? {
        if (hash(slot) == 0) return null
        val f = file(slot)
        return when (slot) {
            in BOSS -> "quest\\boss\\setEmMain\\$f"
            in ESL -> "quest\\zako\\emSetList\\$f"
            in REM -> "quest\\rem\\$f"
            SUPP -> "quest\\supp\\$f"
            PLUS -> "quest\\plus\\questPlus_$f"
            else -> null
        }
    }

    companion object {
        const val VERSION = 0x434B0000
        const val COUNT = 15
        val BOSS = 0..4
        val ESL = 5..7
        val REM = 8..12  // main A, main B, extra A, extra B, sub
        const val SUPP = 13
        const val PLUS = 14

        const val EMPTY_SEM = "b_m00em000_00"
        const val EMPTY_ESL = "z_m00d_000"
        const val EMPTY_REM = "rem_000000"

        val H_SEM = typeHash("rSetEmMain")
        val H_ESL = typeHash("rEmSetList")
        val H_REM = typeHash("rRem")

        fun semName(map: Int, em: Int, id: Int) = "b_m%02dem%03d_%02d".format(map, em, id)
        fun eslName(map: Int, id: Int) = "z_m%02dd_%03d".format(map, id)  // desktop editor drops the 'd' here
        fun remName(id: Int) = "rem_%06d".format(id)
    }
}

/** Large monster spawn point (.sem). */
data class Spawn(val round: Int = 0, val area: Int = 0, val angle: Float = 0f, val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    val isEmpty get() = round == 0 && area == 0 && angle == 0f && x == 0f && y == 0f && z == 0f

    fun write(): ByteArray = ByteArray(32).also {
        it.put32(0, VERSION); it.put32(4, 1)
        it.put32(8, round); it.put32(12, area)
        it.putF(16, angle); it.putF(20, x); it.putF(24, y); it.putF(28, z)
    }

    companion object {
        const val VERSION = 0x3F800000

        fun read(b: ByteArray): Spawn {
            header(b, VERSION, "sem")
            need(b, 32, "sem")
            return Spawn(b.s32(8), b.s32(12), b.f32(16), b.f32(20), b.f32(24), b.f32(28))
        }
    }
}

/** One small monster in an esl pack, 36 bytes. */
class Ems(val raw: ByteArray = blank()) {
    var monster get() = raw.u16(0); set(v) = raw.put16(0, v)
    fun unk1(i: Int) = raw.u8(2 + i)
    fun setUnk1(i: Int, v: Int) = raw.put8(2 + i, v)
    var condition get() = raw.s8(4); set(v) = raw.put8(4, v)
    var area get() = raw.u8(5); set(v) = raw.put8(5, v)
    fun unk2(i: Int) = raw.u16(6 + i * 2)
    fun setUnk2(i: Int, v: Int) = raw.put16(6 + i * 2, v)
    fun pos(i: Int) = raw.f32(12 + i * 4)
    fun setPos(i: Int, v: Float) = raw.putF(12 + i * 4, v)
    var angle get() = raw.f32(24); set(v) = raw.putF(24, v)
    fun unk3(i: Int) = raw.s32(28 + i * 4)
    fun setUnk3(i: Int, v: Int) = raw.put32(28 + i * 4, v)

    fun copy() = Ems(raw.copyOf())

    companion object {
        const val SIZE = 36
        fun blank() = ByteArray(SIZE).also { it.put16(10, 0xCDCD) }
    }
}

/**
 * Small monster set list (.esl). One of the three is out at a time (quest data
 * setType/setTarget/setCount switch to the 2nd and 3rd). Slot i holds the
 * monsters for area i + 1, true for every game file, and empty slots are holes
 * (offset 0), so slots never move. An empty list = unused slot.
 */
class EmSetList(val packs: MutableList<MutableList<Ems>> = MutableList(SLOTS) { mutableListOf() }) {
    val isEmpty get() = packs.all { it.isEmpty() }

    /** first unused slot, -1 when all 14 are taken */
    fun freeSlot() = packs.indexOfFirst { it.isEmpty() }

    /** moves monsters whose area was changed into that area's slot */
    fun regroup() {
        for (i in 0 until SLOTS) {
            val moving = packs[i].filter { it.area in 1..SLOTS && it.area != i + 1 }
            packs[i].removeAll(moving)
            moving.forEach { packs[it.area - 1] += it }
        }
    }

    fun write(): ByteArray {
        val out = ByteArrayOutputStream()
        val head = ByteArray(64)
        head.put32(0, ESL_MAGIC); head.put32(4, ESL_VERSION)
        out.write(head)
        val offsets = IntArray(SLOTS)
        packs.forEachIndexed { i, pack ->
            if (pack.isEmpty()) return@forEachIndexed
            offsets[i] = out.size()
            val h = ByteArray(10)
            h.put32(0, ESD_MAGIC); h.put32(4, ESD_VERSION); h.put16(8, pack.size)
            out.write(h)
            pack.forEach { out.write(it.raw) }
        }
        val b = out.toByteArray()
        offsets.forEachIndexed { i, o -> b.put32(8 + i * 4, o) }
        return b
    }

    companion object {
        const val SLOTS = 14
        const val ESL_MAGIC = 0x6C7365
        const val ESD_MAGIC = 0x445345
        const val ESL_VERSION = 2
        const val ESD_VERSION = 0x20151214

        fun read(b: ByteArray): EmSetList {
            header(b, ESL_MAGIC, "esl")
            need(b, 64, "esl")
            val esl = EmSetList()
            for (i in 0 until SLOTS) {
                val o = b.s32(8 + i * 4)
                if (o <= 0 || o + 10 > b.size || b.s32(o) != ESD_MAGIC) continue
                val n = b.u16(o + 8)
                for (j in 0 until n) {
                    val s = o + 10 + j * Ems.SIZE
                    if (s + Ems.SIZE > b.size) break
                    esl.packs[i] += Ems(b.copyOfRange(s, s + Ems.SIZE))
                }
            }
            return esl
        }
    }
}

/** Monster stat multipliers (em_nando_tbl.nan), read only. */
class StatTable(val rows: List<FloatArray>) {
    companion object {
        fun read(b: ByteArray): StatTable {
            header(b, 0x004E414E, "nan")
            val n = b.s32(8)
            return StatTable(List(n) { i -> FloatArray(8) { j -> b.f32(16 + i * 32 + j * 4) } })
        }
    }
}

/** Arena quest equipment (.ape / rAcPlayerEquip). */
class AcEquip(val quests: MutableList<ArenaQuest>) {

    fun write(): ByteArray {
        val out = ByteArray(8 + quests.size * ArenaQuest.SIZE)
        out.put32(0, VERSION); out.put32(4, quests.size)
        quests.forEachIndexed { i, q -> q.raw.copyInto(out, 8 + i * ArenaQuest.SIZE) }
        return out
    }

    companion object {
        const val VERSION = 0x40000000
        const val ARC_PATH = "quest\\ac_equip\\ac_pl_equip"

        fun read(b: ByteArray): AcEquip {
            header(b, VERSION, "arena equipment")
            val n = b.s32(4)
            need(b, 8 + n * ArenaQuest.SIZE, "arena equipment")
            return AcEquip(MutableList(n) { ArenaQuest(b.copyOfRange(8 + it * ArenaQuest.SIZE, 8 + (it + 1) * ArenaQuest.SIZE)) })
        }
    }
}

class ArenaQuest(val raw: ByteArray) {
    var id get() = raw.s32(0); set(v) = raw.put32(0, v)
    var players get() = raw.u8(4); set(v) = raw.put8(4, v)
    fun rankTime(i: Int) = raw.s32(5 + i * 4)
    fun setRankTime(i: Int, v: Int) = raw.put32(5 + i * 4, v)
    fun rankPoints(i: Int) = raw.s32(17 + i * 4)
    fun setRankPoints(i: Int, v: Int) = raw.put32(17 + i * 4, v)
    val sets = List(5) { EquipSet(raw, 29 + it * EquipSet.SIZE) }
    fun artsFlag(i: Int) = raw.u8(29 + 5 * EquipSet.SIZE + i)
    fun setArtsFlag(i: Int, v: Int) = raw.put8(29 + 5 * EquipSet.SIZE + i, v)

    companion object {
        const val SIZE = 29 + 5 * 182 + 3
    }
}

class EquipSet(private val r: ByteArray, private val o: Int) {
    var weaponType get() = r.u8(o); set(v) = r.put8(o, v)
    var weaponLevel get() = r.u8(o + 1); set(v) = r.put8(o + 1, v)
    var weaponId get() = r.u16(o + 2); set(v) = r.put16(o + 2, v)
    fun weaponDeco(i: Int) = r.u16(o + 4 + i * 2)
    fun setWeaponDeco(i: Int, v: Int) = r.put16(o + 4 + i * 2, v)
    var customAttr get() = r.u8(o + 10); set(v) = r.put8(o + 10, v)
    var style get() = r.u8(o + 11); set(v) = r.put8(o + 11, v)
    fun art(i: Int) = r.u16(o + 12 + i * 2)
    fun setArt(i: Int, v: Int) = r.put16(o + 12 + i * 2, v)
    var charmSkill1 get() = r.u8(o + 18); set(v) = r.put8(o + 18, v)
    var charmLevel1 get() = r.s8(o + 19); set(v) = r.put8(o + 19, v)
    var charmSkill2 get() = r.u8(o + 20); set(v) = r.put8(o + 20, v)
    var charmLevel2 get() = r.s8(o + 21); set(v) = r.put8(o + 21, v)

    // kinsect: id, level, power, stamina, speed, fire, water, thunder, ice, dragon
    fun insect(i: Int) = r.u8(o + 22 + i)
    fun setInsect(i: Int, v: Int) = r.put8(o + 22 + i, v)

    // 0-4 armor, 5 = charm (only decorations used)
    fun armorId(i: Int) = r.u16(o + 32 + i * 9)
    fun setArmorId(i: Int, v: Int) = r.put16(o + 32 + i * 9, v)
    fun armorLevel(i: Int) = r.u8(o + 34 + i * 9)
    fun setArmorLevel(i: Int, v: Int) = r.put8(o + 34 + i * 9, v)
    fun armorDeco(i: Int, d: Int) = r.u16(o + 35 + i * 9 + d * 2)
    fun setArmorDeco(i: Int, d: Int, v: Int) = r.put16(o + 35 + i * 9 + d * 2, v)

    fun itemId(i: Int) = r.u16(o + 86 + i * 3)
    fun setItemId(i: Int, v: Int) = r.put16(o + 86 + i * 3, v)
    fun itemCount(i: Int) = r.u8(o + 88 + i * 3)
    fun setItemCount(i: Int, v: Int) = r.put8(o + 88 + i * 3, v)

    companion object {
        const val SIZE = 182
        const val ITEMS = 32
    }
}

/**
 * Supply box list (.sup): 40 slots of {item u16, count u8, section u8}.
 * A slot whose section byte is non-zero starts that section, everything
 * before the first marker is section 0 (the start box). Each supply set in
 * the quest data hands out one section by number (QuestData.suppLabel).
 * Checked against every quest in the game.
 */
class Supply(val raw: ByteArray) {
    init {
        header(raw, VERSION, "supply")
        need(raw, 8 + SLOTS * 4, "supply")
    }

    fun item(i: Int) = raw.u16(8 + i * 4)
    fun setItem(i: Int, v: Int) = raw.put16(8 + i * 4, v)
    fun count(i: Int) = raw.u8(10 + i * 4)
    fun setCount(i: Int, v: Int) = raw.put8(10 + i * 4, v)
    fun marker(i: Int) = raw.u8(11 + i * 4)
    fun setMarker(i: Int, v: Int) = raw.put8(11 + i * 4, v)

    /** section number to slot range, in file order */
    fun sections(): List<Pair<Int, IntRange>> {
        val starts = (0 until SLOTS).filter { it == 0 || marker(it) != 0 }
        return starts.mapIndexed { n, st ->
            val end = if (n + 1 < starts.size) starts[n + 1] - 1 else SLOTS - 1
            marker(st) to st..end
        }
    }

    companion object {
        const val VERSION = 0x3F800000
        const val SLOTS = 40
    }
}

/**
 * QuestPlus (.qdp), 17 bytes. Only used on arena, slayground, fortress and
 * castle maps. Names below come from comparing all 1849 game quests:
 *   0x00 u8  arena fence on (1 on every arena / slayground quest with fences)
 *   0x01 u8  barricades on (Fortress Ruins)
 *   0x02 u16 fence timer 1 (90, 40 in G rank)
 *   0x04 u16 always 0
 *   0x06 u16 fence timer 2 (20)
 *   0x08 u8  set on slayground, Castle Schrade, fortress, citadel: siege weapons?
 *   0x09 u16 always 0
 *   0x0B u16 600 on some slayground and Fatalis quests: weapon timer?
 *   0x0D u16 front barricade HP (600, quest fails when it hits 0)
 *   0x0F u16 main barricade HP (600)
 */
class QuestPlus(val raw: ByteArray) {
    init {
        header(raw, VERSION, "quest plus")
        need(raw, 8 + SIZE, "quest plus")
    }

    fun u8(o: Int) = raw.u8(8 + o)
    fun setU8(o: Int, v: Int) = raw.put8(8 + o, v)
    fun u16(o: Int) = raw.u16(8 + o)
    fun setU16(o: Int, v: Int) = raw.put16(8 + o, v)

    companion object {
        const val VERSION = 0x3F800000
        const val SIZE = 17
    }
}
