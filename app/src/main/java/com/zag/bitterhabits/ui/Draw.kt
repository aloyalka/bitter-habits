package com.zag.bitterhabits.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zag.bitterhabits.data.DayState
import kotlin.math.floor
import kotlin.math.sqrt

private val pathCache = HashMap<String, Path>()
fun parsedPath(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

@Composable
fun EmblemIcon(key: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val e = Art.emblem(key)
    Canvas(modifier.size(size)) {
        val sc = this.size.width / 24f
        scale(sc, sc, pivot = Offset.Zero) {
            e.strokes.forEach {
                drawPath(parsedPath(it), color, style = Stroke(width = 1.8f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
            }
            e.fills.forEach { drawPath(parsedPath(it), color) }
        }
    }
}

@Composable
fun Logo(height: Dp, modifier: Modifier = Modifier, letters: Color = C.tx, squiggle: Color = C.lime) {
    val width = height * (Art.LOGO_W / Art.LOGO_H)
    Canvas(modifier.size(width, height)) {
        val sc = this.size.height / Art.LOGO_H
        scale(sc, sc, pivot = Offset.Zero) {
            translate(-Art.LOGO_X, -Art.LOGO_Y) {
                Art.logoLetters.forEach { drawPath(parsedPath(it), letters) }
                drawPath(parsedPath(Art.logoSquiggle), squiggle)
            }
        }
    }
}

// ---------- week strip: seven rounded squares, or one squiggle for a perfect week ----------

class StripGeom(val s: Float, val p: Float, val sx: Float, val h: Float, val sw: Float) {
    val u = p / 2f
    val sy = (h - s) / 2f
    val off = sx + s / 2f - 1.5f * u
    val oy = (h - 3f * u) / 2f
    val r = s * 0.27f
    fun cx(k: Int) = sx + k * p + s / 2f
    val cy = h / 2f
    fun index(x: Float): Int = floor((x - sx + (p - s) / 2f) / p).toInt().coerceIn(0, 6)
}

fun StripGeom.poly(pts: List<Pair<Float, Float>>): Path {
    val path = Path()
    pts.forEachIndexed { i, (x, y) ->
        val px = off + x * u
        val py = oy + y * u
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

fun DrawScope.drawCell(st: DayState, left: Float, top: Float, s: Float, r: Float, color: Color, sw: Float, big: Boolean) {
    val half = sw / 2f
    fun outline(c: Color) = drawRoundRect(
        c, Offset(left + half, top + half), Size(s - sw, s - sw), CornerRadius(r - half), style = Stroke(sw),
    )
    when (st) {
        DayState.DONE -> drawRoundRect(color, Offset(left, top), Size(s, s), CornerRadius(r))
        DayState.REST -> {
            outline(color)
            val ins = if (big) s * 0.19f else s * 0.3f
            drawRoundRect(color, Offset(left + ins, top + ins), Size(s - 2 * ins, s - 2 * ins), CornerRadius(r * 0.35f))
        }
        DayState.TODAY -> outline(C.tx)
        DayState.OPEN -> outline(C.open)
        DayState.FUTURE -> outline(C.future)
        DayState.NONE -> {}
    }
}

/** Seven whole rectangles edge to edge: rest days faded, done days solid. */
fun DrawScope.drawSquiggle(states: List<DayState>, g: StripGeom, color: Color, glow: Float = 0f) {
    val whole = g.poly(Art.run(0, 6))
    if (glow > 0f) drawPath(whole, color.copy(alpha = 0.35f * glow), style = Stroke(width = g.u * 0.7f, join = StrokeJoin.Miter))
    drawPath(whole, color.copy(alpha = 0.3f))
    Art.runs(states.map { it == DayState.DONE }).forEach { r -> drawPath(g.poly(Art.run(r.first, r.last)), color) }
}

private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) 4f * x * x * x else 1f - ((-2f * x + 2f) * (-2f * x + 2f) * (-2f * x + 2f)) / 2f
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Morph squares into the squiggle. T runs from 0 (squares) past 1 (squiggle). */
fun DrawScope.drawMorph(states: List<DayState>, g: StripGeom, color: Color, T: Float) {
    val ts = (0..6).map { ease((T - it * 0.07f) / 0.55f) }
    if (ts.all { it >= 1f }) {
        val glow = if (T < 2f) kotlin.math.sin(((T - 0.8f) / 1.2f).coerceIn(0f, 1f) * Math.PI.toFloat()) else 0f
        drawSquiggle(states, g, color, glow)
        return
    }
    val root2 = sqrt(2f)
    states.forEachIndexed { k, st ->
        val t = ts[k]
        val cx = g.cx(k)
        val cy = g.cy
        val w = lerp(g.s, 2f * root2 * g.u, t)
        val h = lerp(g.s, root2 * g.u, t)
        val r = lerp(g.r, 0f, t)
        rotate(45f * t, pivot = Offset(cx, cy)) {
            val tl = Offset(cx - w / 2f, cy - h / 2f)
            when (st) {
                DayState.DONE -> drawRoundRect(color, tl, Size(w, h), CornerRadius(r))
                DayState.REST -> {
                    drawRoundRect(color.copy(alpha = 0.3f * t), tl, Size(w, h), CornerRadius(r))
                    val half = g.sw / 2f
                    drawRoundRect(
                        color.copy(alpha = 1f - t), Offset(tl.x + half, tl.y + half), Size(w - g.sw, h - g.sw),
                        CornerRadius((r - half).coerceAtLeast(0f)), style = Stroke(g.sw),
                    )
                    val iw = g.s * 0.4f * (1f - t)
                    if (iw > 0.5f) drawRoundRect(color.copy(alpha = 1f - t), Offset(cx - iw / 2f, cy - iw / 2f), Size(iw, iw), CornerRadius(iw * 0.2f))
                }
                else -> drawCell(st, cx - g.s / 2f, cy - g.s / 2f, g.s, g.r, color, g.sw, false)
            }
        }
    }
}

@Composable
fun WeekStrip(
    states: List<DayState>,
    perfect: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    morphT: Float? = null,
    cell: Dp = 22.dp,
    pitch: Dp = 26.dp,
    startPad: Dp = 10.dp,
    height: Dp = 64.dp,
    numbers: List<Int?>? = null,
    onTap: ((Int) -> Unit)? = null,
    onLongPress: ((Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val width = startPad + cell / 2 + pitch * 6.75f + 2.dp
    val big = cell >= 30.dp
    Canvas(
        modifier
            .size(width, height)
            .pointerInput(Unit) {
                val sx = startPad.toPx(); val s = cell.toPx(); val p = pitch.toPx()
                fun idx(x: Float) = floor((x - sx + (p - s) / 2f) / p).toInt().coerceIn(0, 6)
                detectTapGestures(
                    onTap = { o -> tap?.invoke(idx(o.x)) },
                    onLongPress = { o -> long?.invoke(idx(o.x)) },
                )
            },
    ) {
        val g = StripGeom(cell.toPx(), pitch.toPx(), startPad.toPx(), size.height, 1.5.dp.toPx())
        when {
            morphT != null -> drawMorph(states, g, color, morphT)
            perfect -> drawSquiggle(states, g, color)
            else -> states.forEachIndexed { k, st -> drawCell(st, g.sx + k * g.p, g.sy, g.s, g.r, color, g.sw, big) }
        }
        numbers?.forEachIndexed { k, n ->
            if (n == null) return@forEachIndexed
            val st = states[k]
            val c = when {
                st == DayState.DONE -> C.bg
                st == DayState.REST && !perfect && morphT == null -> C.bg
                st == DayState.REST -> C.tx
                st == DayState.FUTURE -> C.tx3
                else -> Color(0xFFC9D1C6)
            }
            val layout = measurer.measure(n.toString(), ts(11.sp, FontWeight.SemiBold))
            drawText(layout, color = c, topLeft = Offset(g.cx(k) - layout.size.width / 2f, g.cy - layout.size.height / 2f))
        }
    }
}
