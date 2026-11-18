package com.zag.bitterhabits.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.zag.bitterhabits.data.DayState
import com.zag.bitterhabits.ui.Art

/** Draws the widget's pieces as bitmaps, using the same geometry as the app. */
object WidgetArt {
    private const val OPEN = 0xFF3E4842.toInt()
    private const val FUTURE = 0xFF232B26.toInt()
    private const val TODAY = 0xFFF2F5EE.toInt()

    private fun density(ctx: Context) = ctx.resources.displayMetrics.density.coerceAtMost(3f)

    private fun withAlpha(color: Int, a: Float): Int =
        (((color ushr 24) * a).toInt() shl 24) or (color and 0x00FFFFFF)

    /** Seven squares, or one squiggle for a perfect week. Sizes in dp. */
    fun strip(
        ctx: Context, states: List<DayState>, perfect: Boolean, color: Int,
        cell: Float = 14f, pitch: Float = 17f, pad: Float = 6f, height: Float = 28f, width: Float = 130f,
    ): Bitmap {
        val d = density(ctx)
        val bmp = Bitmap.createBitmap((width * d).toInt(), (height * d).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = cell * d; val p = pitch * d; val sx = pad * d; val h = height * d; val sw = 1.3f * d
        val r = s * 0.27f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (perfect) {
            val u = p / 2f
            val off = sx + s / 2f - 1.5f * u
            val oy = (h - 3f * u) / 2f
            fun poly(pts: List<Pair<Float, Float>>): Path {
                val path = Path()
                pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(off + x * u, oy + y * u) else path.lineTo(off + x * u, oy + y * u) }
                path.close(); return path
            }
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(color, 0.3f)
            c.drawPath(poly(Art.run(0, 6)), paint)
            paint.color = color
            Art.runs(states.map { it == DayState.DONE }).forEach { run -> c.drawPath(poly(Art.run(run.first, run.last)), paint) }
            return bmp
        }
        val top = (h - s) / 2f
        states.forEachIndexed { k, st ->
            val left = sx + k * p
            fun outline(col: Int) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = sw; paint.color = col
                c.drawRoundRect(RectF(left + sw / 2, top + sw / 2, left + s - sw / 2, top + s - sw / 2), r - sw / 2, r - sw / 2, paint)
            }
            when (st) {
                DayState.DONE -> {
                    paint.style = Paint.Style.FILL; paint.color = color
                    c.drawRoundRect(RectF(left, top, left + s, top + s), r, r, paint)
                }
                DayState.REST -> {
                    outline(color)
                    val ins = s * 0.3f
                    paint.style = Paint.Style.FILL; paint.color = color
                    c.drawRoundRect(RectF(left + ins, top + ins, left + s - ins, top + s - ins), r * 0.35f, r * 0.35f, paint)
                }
                DayState.TODAY -> outline(TODAY)
                DayState.OPEN -> outline(OPEN)
                DayState.FUTURE -> outline(FUTURE)
                DayState.NONE -> {}
            }
        }
        return bmp
    }

    fun emblem(ctx: Context, key: String, color: Int, sizeDp: Float): Bitmap {
        val d = density(ctx)
        val px = (sizeDp * d).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val sc = px / 24f
        val m = Matrix().apply { setScale(sc, sc) }
        val e = Art.emblem(key)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f * sc
            strokeCap = Paint.Cap.SQUARE; strokeJoin = Paint.Join.MITER
        }
        e.strokes.forEach { c.drawPath(PathParser.createPathFromPathData(it).apply { transform(m) }, paint) }
        paint.style = Paint.Style.FILL
        e.fills.forEach { c.drawPath(PathParser.createPathFromPathData(it).apply { transform(m) }, paint) }
        return bmp
    }

    fun logo(ctx: Context, heightDp: Float): Bitmap {
        val d = density(ctx)
        val h = heightDp * d
        val w = h * Art.LOGO_W / Art.LOGO_H
        val bmp = Bitmap.createBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val sc = h / Art.LOGO_H
        val m = Matrix().apply { setTranslate(-Art.LOGO_X, -Art.LOGO_Y); postScale(sc, sc) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = TODAY }
        Art.logoLetters.forEach { c.drawPath(PathParser.createPathFromPathData(it).apply { transform(m) }, paint) }
        paint.color = 0xFFE0F72E.toInt()
        c.drawPath(PathParser.createPathFromPathData(Art.logoSquiggle).apply { transform(m) }, paint)
        return bmp
    }
}
