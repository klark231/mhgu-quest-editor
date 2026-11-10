package io.github.klark231.mhguqe

import java.io.IOException

/**
 * Area map textures from the game's stage/mapUI/mXXd/aYY_ui_*.arc files.
 * They're Switch TEX files: BC3, block-linear swizzled, 256x256.
 */
object StageMaps {
    // stage/mapUI/m07d/a03_ui_eng.arc -> (7, 3), night and event variants are skipped
    private val NAME = Regex("""(?:^|/)m(\d\d)d/a(\d\d)_ui_\w+\.arc$""")

    fun areaOf(path: String): Pair<Int, Int>? =
        NAME.find(path.replace('\\', '/'))?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }

    /** ARGB pixels of the area map in an mapUI arc, null if there's none */
    fun decode(arcBytes: ByteArray): Triple<Int, Int, IntArray>? {
        val arc = Arc.read(arcBytes)
        val e = arc.entries.firstOrNull { it.name.startsWith("HD_m") && it.name.endsWith("_BM_NOMIP") } ?: return null
        return decodeTex(e.bytes())
    }

    fun decodeTex(t: ByteArray): Triple<Int, Int, IntArray> {
        if (t.size < 24 || t.s32(0) != 0x584554) throw IOException("not a TEX file")
        val w2 = t.s32(8)
        val w3 = t.s32(12)
        val width = (w2 ushr 6) and 0x1FFF
        val height = (w2 ushr 19) and 0x1FFF
        val format = (w3 ushr 8) and 0xFF
        if (format != 0x17) throw IOException("map texture format 0x%02x isn't handled".format(format))
        val wb = (width + 3) / 4
        val hb = (height + 3) / 4
        val blocks = deswizzle(t, 24, wb, hb, 16)
        val px = IntArray(width * height)
        for (by in 0 until hb) for (bx in 0 until wb) bc3(blocks, (by * wb + bx) * 16, px, width, height, bx * 4, by * 4)
        return Triple(width, height, px)
    }

    // Tegra X1 block linear layout, same as Switch Toolbox does it
    private fun deswizzle(src: ByteArray, base: Int, wb: Int, hb: Int, bpp: Int): ByteArray {
        // smallest power of two with bh * 8 >= height in blocks, at most 16
        var bh = 1
        while (bh < 16 && bh * 8 < hb) bh *= 2
        val gobsWide = (wb * bpp + 63) / 64
        val out = ByteArray(wb * hb * bpp)
        for (y in 0 until hb) for (x in 0 until wb) {
            val xb = x * bpp
            val gob = (y / (8 * bh)) * 512 * bh * gobsWide + (xb / 64) * 512 * bh + (y % (8 * bh) / 8) * 512
            val a = gob + ((xb % 64) / 32) * 256 + ((y % 8) / 2) * 64 + ((xb % 32) / 16) * 32 + (y % 2) * 16 + (xb % 16)
            if (base + a + bpp <= src.size) System.arraycopy(src, base + a, out, (y * wb + x) * bpp, bpp)
        }
        return out
    }

    private fun bc3(b: ByteArray, o: Int, px: IntArray, w: Int, h: Int, x0: Int, y0: Int) {
        val a0 = b.u8(o)
        val a1 = b.u8(o + 1)
        val alpha = IntArray(8)
        alpha[0] = a0; alpha[1] = a1
        if (a0 > a1) for (i in 1..6) alpha[i + 1] = ((7 - i) * a0 + i * a1) / 7
        else {
            for (i in 1..4) alpha[i + 1] = ((5 - i) * a0 + i * a1) / 5
            alpha[6] = 0; alpha[7] = 255
        }
        var abits = 0L
        for (i in 0 until 6) abits = abits or (b.u8(o + 2 + i).toLong() shl (8 * i))

        val c0 = b.u16(o + 8)
        val c1 = b.u16(o + 10)
        val col = IntArray(4)
        col[0] = rgb565(c0); col[1] = rgb565(c1)
        col[2] = mix(col[0], col[1], 2, 1)
        col[3] = mix(col[0], col[1], 1, 2)
        val cbits = b.s32(o + 12)

        for (i in 0 until 16) {
            val x = x0 + (i and 3)
            val y = y0 + (i shr 2)
            if (x >= w || y >= h) continue
            val a = alpha[((abits ushr (3 * i)) and 7).toInt()]
            val c = col[(cbits ushr (2 * i)) and 3]
            px[y * w + x] = (a shl 24) or c
        }
    }

    private fun rgb565(c: Int): Int {
        val r = (c ushr 11) and 31
        val g = (c ushr 5) and 63
        val b = c and 31
        return ((r shl 3 or (r shr 2)) shl 16) or ((g shl 2 or (g shr 4)) shl 8) or (b shl 3 or (b shr 2))
    }

    private fun mix(p: Int, q: Int, wp: Int, wq: Int): Int {
        fun ch(s: Int) = (((p shr s) and 255) * wp + ((q shr s) and 255) * wq) / (wp + wq)
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
