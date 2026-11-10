package io.github.klark231.mhguqe

import java.io.IOException

/**
 * Rewards and supplies as plain text, one row per line. Numbers first, the
 * item name after them is only there for people reading the file.
 * Lines starting with # are skipped.
 */
object TextLists {

    fun rewards(r: Rem, name: (Int) -> String): String = buildString {
        append("# item\tamount\tchance\tname\n")
        for (w in r.rewards()) append("${w.item}\t${w.amount}\t${w.weight}\t${name(w.item)}\n")
    }

    fun parseRewards(t: String): List<Rem.Reward> {
        val out = rows(t, 3).map { (n, v) ->
            check(n, v[0] in 0..65535 && v[1] in 0..255 && v[2] in 0..254)
            Rem.Reward(v[0], v[1], v[2])
        }
        if (out.size > Rem.MAX - 1) throw IOException("more than ${Rem.MAX - 1} rewards")
        return out
    }

    fun supply(s: Supply, name: (Int) -> String): String = buildString {
        append("# slot\titem\tamount\tsection\tname\n")
        for (i in 0 until Supply.SLOTS) {
            if (s.item(i) == 0 && s.count(i) == 0 && s.marker(i) == 0) continue
            append("${i + 1}\t${s.item(i)}\t${s.count(i)}\t${s.marker(i)}\t${name(s.item(i))}\n")
        }
    }

    /** replaces every slot, ones not in the text end up empty */
    fun applySupply(t: String, s: Supply) {
        val rows = rows(t, 4)
        rows.forEach { (n, v) -> check(n, v[0] in 1..Supply.SLOTS && v[1] in 0..65535 && v[2] in 0..255 && v[3] in 0..255) }
        for (i in 0 until Supply.SLOTS) { s.setItem(i, 0); s.setCount(i, 0); s.setMarker(i, 0) }
        for ((_, v) in rows) {
            val i = v[0] - 1
            s.setItem(i, v[1]); s.setCount(i, v[2]); s.setMarker(i, if (i == 0) 0 else v[3])
        }
    }

    // (line number, first n numbers) for every line that has data
    private fun rows(t: String, n: Int): List<Pair<Int, List<Int>>> =
        t.lines().mapIndexedNotNull { i, line ->
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) return@mapIndexedNotNull null
            val parts = l.split('\t', ',', ';', ' ').filter { it.isNotBlank() }
            val nums = parts.take(n).map { it.trim().toIntOrNull() ?: throw IOException("line ${i + 1}: \"$it\" isn't a number") }
            if (nums.size < n) throw IOException("line ${i + 1}: needs $n numbers")
            (i + 1) to nums
        }

    private fun check(line: Int, ok: Boolean) {
        if (!ok) throw IOException("line $line: value out of range")
    }
}
