package io.github.klark231.mhguqe

/** one quest as the quest lists describe it, used for the browser on the home screen */
data class QuestInfo(
    val id: Int,
    val list: String,          // vq, gq, gq2, tq, or "" when it's only a q file
    val level: Int,
    val map: Int,
    val icon: Int,
    val names: List<String>,   // per game language, may be empty strings
    val file: String = "",     // uri of qXXXXXXX.arc when the folder has it
) {
    fun name(lang: Int) = names.getOrNull(lang)?.takeIf { it.isNotBlank() && it != "@" }
        ?: names.firstOrNull { it.isNotBlank() && it != "@" } ?: "q%07d".format(id)
}

object QuestIndex {
    // the game's quest lists. A quest only shows up in game when its data and text are in one of these.
    val LISTS = listOf("vq", "gq", "gq2", "tq")
    private val Q_FILE = Regex("""^q(\d{7})\.arc$""")

    fun questId(fileName: String) = Q_FILE.find(fileName)?.groupValues?.get(1)?.toInt()
    fun listName(fileName: String) = fileName.removeSuffix(".arc").takeIf { fileName.endsWith(".arc") && it in LISTS }

    fun read(list: String, arc: Arc): List<QuestInfo> {
        val out = ArrayList<QuestInfo>()
        for (e in arc.entries) {
            if (!e.path.startsWith("loc\\quest\\questData\\questData_")) continue
            val d = try { QuestData(e.bytes()) } catch (_: Exception) { continue }
            val names = List(7) { lang ->
                val f = d.infoFile(lang)
                val g = arc.find("${Lang.codes[lang]}\\quest\\questData\\questData_$f")
                g?.let { runCatching { Gmd.read(it.bytes()).entries.firstOrNull() }.getOrNull() } ?: ""
            }
            out += QuestInfo(d.id, list, d.level, d.map, d.icon(0), names)
        }
        return out
    }

    // a small tab separated file, one quest per line
    fun save(all: List<QuestInfo>): String = buildString {
        for (q in all) {
            append(q.id).append('\t').append(q.list).append('\t').append(q.level).append('\t').append(q.map).append('\t')
            append(q.icon).append('\t').append(q.file)
            for (n in q.names) append('\t').append(n.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '))
            append('\n')
        }
    }

    fun load(text: String): List<QuestInfo> = text.lineSequence().filter { it.isNotBlank() }.mapNotNull { l ->
        val p = l.split('\t')
        if (p.size < 6) return@mapNotNull null
        QuestInfo(p[0].toInt(), p[1], p[2].toInt(), p[3].toInt(), p[4].toInt(), p.drop(6), p[5])
    }.toList()
}
