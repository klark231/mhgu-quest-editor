@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.klark231.mhguqe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/** a picked spot; angle is only known when it came from a spawn preset */
class Picked(val area: Int, val x: Float, val y: Float, val z: Float, val angle: Float?)

private class Dot(val x: Float, val y: Float, val z: Float, val angle: Float?)

// world <-> canvas for one area. With a calibrated map image it uses the fit,
// otherwise it just frames the known points.
private class View(dots: List<Dot>, private val cal: FloatArray?) {
    private val minX: Float
    private val minZ: Float
    private val span: Float

    init {
        val xs = dots.map { it.x }
        val zs = dots.map { it.z }
        val w = ((xs.maxOrNull() ?: 0f) - (xs.minOrNull() ?: 0f)).coerceAtLeast(100f)
        val h = ((zs.maxOrNull() ?: 0f) - (zs.minOrNull() ?: 0f)).coerceAtLeast(100f)
        span = maxOf(w, h) * 1.25f
        minX = (xs.minOrNull() ?: 0f) - (span - w) / 2
        minZ = (zs.minOrNull() ?: 0f) - (span - h) / 2
    }

    fun toCanvas(x: Float, z: Float, size: Float): Offset =
        if (cal != null) Offset((x * cal[0] + cal[1]) * size / 256f, (z * cal[0] + cal[2]) * size / 256f)
        else Offset((x - minX) / span * size, (z - minZ) / span * size)

    fun toWorld(o: Offset, size: Float): Pair<Float, Float> =
        if (cal != null) ((o.x * 256f / size - cal[1]) / cal[0]) to ((o.y * 256f / size - cal[2]) / cal[0])
        else (o.x / size * span + minX) to (o.y / size * span + minZ)
}

@Composable
fun MapPickerDialog(vm: AppViewModel, map: Int, area: Int, onPick: (Picked) -> Unit, onDismiss: () -> Unit) {
    val g = vm.game
    val areas = remember(map) { g.knownAreas(map) }
    var sel by remember { mutableIntStateOf(if (area in areas) area else areas.firstOrNull() ?: area) }
    var picked by remember { mutableStateOf<Picked?>(null) }
    val dots = remember(map, sel) {
        g.positions(map, sel).map { Dot(it[0], it[1], it[2], null) } +
            g.spawnPresets.filter { it.map == map && it.area == sel }.map { Dot(it.spawn.x, it.spawn.y, it.spawn.z, it.spawn.angle) }
    }
    val image = remember(map, sel, vm.mapCount) { vm.mapImage(map, sel) }
    val cal = if (image != null) g.calibration(map, sel) else null
    val view = remember(dots, cal) { View(dots, cal) }
    val touch = with(LocalDensity.current) { 22.dp.toPx() }
    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_spawn) + " · " + g.mapName(map)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (areas.isEmpty()) Text(stringResource(R.string.no_presets, g.mapName(map)))
                else ChipBar(areas.map { stringResource(R.string.area_n, it) }, areas.indexOf(sel).coerceAtLeast(0), { sel = areas[it]; picked = null })
                if (areas.isNotEmpty()) Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHighest),
                ) {
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(dots, cal) {
                            detectTapGestures { tap ->
                                val size = this.size.width.toFloat()
                                val near = dots.minByOrNull { val c = view.toCanvas(it.x, it.z, size); hypot(c.x - tap.x, c.y - tap.y) }
                                val nearDist = near?.let { val c = view.toCanvas(it.x, it.z, size); hypot(c.x - tap.x, c.y - tap.y) }
                                picked = if (near != null && nearDist!! <= touch) {
                                    Picked(sel, near.x, near.y, near.z, near.angle)
                                } else {
                                    val (x, z) = view.toWorld(tap, size)
                                    // height from the closest real spot, a guessed Y puts monsters underground
                                    val ref = dots.minByOrNull { hypot(it.x - x, it.z - z) }
                                    Picked(sel, x, ref?.y ?: 0f, z, null)
                                }
                            }
                        },
                    ) {
                        val size = this.size.width
                        if (image != null) drawImage(image, dstSize = IntSize(size.toInt(), size.toInt()), filterQuality = FilterQuality.None)
                        for (d in dots) {
                            val c = view.toCanvas(d.x, d.z, size)
                            drawCircle(if (d.angle != null) cs.tertiary else Color(0xFFE53935), 4.dp.toPx(), c)
                        }
                        picked?.let { p ->
                            val c = view.toCanvas(p.x, p.z, size)
                            drawCircle(Color.White, 9.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                            drawCircle(cs.primary, 6.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                        }
                    }
                }
                if (areas.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendRow(Color(0xFFE53935), false, stringResource(R.string.legend_quest))
                    if (dots.any { it.angle != null }) LegendRow(cs.tertiary, false, stringResource(R.string.legend_preset))
                    LegendRow(cs.primary, true, stringResource(R.string.legend_pick))
                }
                Hint(stringResource(if (image != null && cal != null) R.string.map_tap_hint else R.string.map_tap_hint_plain))
                picked?.let {
                    Text(
                        "X %.0f · Y %.0f · Z %.0f".format(it.x, it.y, it.z) + (it.angle?.let { a -> " · %.0f°".format(a) } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { picked?.let(onPick) }, enabled = picked != null) { Text(stringResource(R.string.use)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LegendRow(color: Color, ring: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(16.dp)) {
            if (ring) {
                drawCircle(Color.White, 7.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(color, 5.dp.toPx(), style = Stroke(2.dp.toPx()))
            } else drawCircle(color, 4.dp.toPx())
        }
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
