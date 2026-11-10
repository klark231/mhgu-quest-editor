package io.github.klark231.mhguqe

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

fun typeHash(name: String): Int {
    val c = CRC32()
    c.update(name.toByteArray(Charsets.ISO_8859_1))
    // MT uses ~crc, java gives the finalized one, so flip it back
    return (c.value.toInt().inv()) and 0x7FFFFFFF
}

// little endian helpers on plain byte arrays
fun ByteArray.u8(o: Int) = this[o].toInt() and 0xFF
fun ByteArray.s8(o: Int) = this[o].toInt()
fun ByteArray.u16(o: Int) = u8(o) or (u8(o + 1) shl 8)
fun ByteArray.s32(o: Int) = u16(o) or (u16(o + 2) shl 16)
fun ByteArray.u32(o: Int) = s32(o).toLong() and 0xFFFFFFFFL
fun ByteArray.f32(o: Int) = Float.fromBits(s32(o))

fun ByteArray.put8(o: Int, v: Int) { this[o] = v.toByte() }
fun ByteArray.put16(o: Int, v: Int) { this[o] = v.toByte(); this[o + 1] = (v shr 8).toByte() }
fun ByteArray.put32(o: Int, v: Int) { put16(o, v); put16(o + 2, v ushr 16) }
fun ByteArray.putF(o: Int, v: Float) = put32(o, v.toRawBits())

fun ByteArray.cstr(o: Int, max: Int): String {
    var n = 0
    while (n < max && this[o + n] != 0.toByte()) n++
    return String(this, o, n, Charsets.UTF_8)
}

fun ByteArray.putCstr(o: Int, max: Int, s: String) {
    val b = s.toByteArray(Charsets.UTF_8)
    val n = minOf(b.size, max - 1)
    fill(0, o, o + max)
    b.copyInto(this, o, 0, n)
}

class ArcEntry(
    var path: String,
    val typeHash: Int,
    var data: ByteArray,  // as stored, usually zlib
    var realSize: Int,
    var quality: Int = 2,
) {
    val ext: String get() = ExtTable.resolve(typeHash)
    val name: String get() = path.substringAfterLast('\\')

    fun bytes(): ByteArray {
        if (data.size == realSize) {
            // some tools store things uncompressed, zlib streams start with 0x78
            if (data.isEmpty() || data[0] != 0x78.toByte()) return data.copyOf()
        }
        val inf = Inflater()
        try {
            inf.setInput(data)
            val out = ByteArray(realSize)
            var n = 0
            while (n < realSize) {
                val r = inf.inflate(out, n, realSize - n)
                if (r == 0 && (inf.finished() || inf.needsInput())) break
                n += r
            }
            if (n != realSize) throw IOException("$path: unpacked $n bytes, expected $realSize")
            return out
        } finally {
            inf.end()
        }
    }

    fun set(raw: ByteArray) {
        data = deflate(raw)
        realSize = raw.size
    }

    // copies the stored bytes as they are, no repacking
    fun copyStored(from: ArcEntry) {
        data = from.data.copyOf()
        realSize = from.realSize
    }

    companion object {
        fun deflate(raw: ByteArray): ByteArray {
            val d = Deflater(Deflater.DEFAULT_COMPRESSION)
            try {
                d.setInput(raw)
                d.finish()
                val out = ByteArrayOutputStream(raw.size / 2 + 64)
                val buf = ByteArray(16384)
                while (!d.finished()) out.write(buf, 0, d.deflate(buf))
                return out.toByteArray()
            } finally {
                d.end()
            }
        }
    }
}

class Arc(val entries: MutableList<ArcEntry>) {

    fun find(path: String) = entries.firstOrNull { it.path.equals(path, ignoreCase = true) }
    fun firstByExt(ext: String) = entries.firstOrNull { it.ext == ext }

    fun add(path: String, type: String, raw: ByteArray): ArcEntry {
        val e = ArcEntry(path, typeHash(type), ByteArray(0), 0)
        e.set(raw)
        entries += e
        return e
    }

    fun write(order: List<ArcEntry> = entries): ByteArray {
        val tableEnd = 12 + order.size * 80
        // game arcs start their data on a 0x8000 boundary
        val dataStart = (tableEnd + ALIGN - 1) / ALIGN * ALIGN
        val out = ByteArray(dataStart + order.sumOf { it.data.size })
        out.put32(0, MAGIC)
        out.put16(4, VERSION)
        out.put16(6, order.size)
        var off = dataStart
        order.forEachIndexed { i, e ->
            val p = 12 + i * 80
            out.putCstr(p, 64, e.path)
            out.put32(p + 64, e.typeHash)
            out.put32(p + 68, e.data.size)
            out.put32(p + 72, (e.realSize and 0x1FFFFFFF) or (e.quality shl 29))
            out.put32(p + 76, off)
            e.data.copyInto(out, off)
            off += e.data.size
        }
        return out
    }

    companion object {
        const val MAGIC = 0x435241
        const val VERSION = 0x11
        const val ALIGN = 0x8000

        fun read(b: ByteArray): Arc {
            if (b.size < 12 || b.s32(0) != MAGIC) throw IOException("not an ARC file")
            if (b.u16(4) != VERSION) throw IOException("unsupported ARC version 0x%x".format(b.u16(4)))
            val n = b.u16(6)
            val list = ArrayList<ArcEntry>(n)
            for (i in 0 until n) {
                val p = 12 + i * 80
                val comp = b.s32(p + 68)
                val packed = b.s32(p + 72)
                val off = b.s32(p + 76)
                if (off < 0 || comp < 0 || off + comp > b.size) throw IOException("ARC entry $i points outside the file")
                list += ArcEntry(
                    path = b.cstr(p, 64),
                    typeHash = b.s32(p + 64),
                    data = b.copyOfRange(off, off + comp),
                    realSize = packed and 0x1FFFFFFF,
                    quality = packed ushr 29,
                )
            }
            return Arc(list)
        }
    }
}
