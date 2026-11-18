package com.zag.bitterhabits.ui

/**
 * Vector art shared by the app (Compose) and the widget (bitmaps).
 * All emblem paths sit on a 24 x 24 grid and are drawn with a 1.8 stroke, square caps, mitred corners.
 */
object Art {

    // ---------- habit colours, handed out in this order ----------
    val habitColors: List<Long> = listOf(0xFFE0F72E, 0xFFE23CFF, 0xFFFF7A1F, 0xFF3CD7FF, 0xFFF4F6F0)
    val habitColorNames = listOf("Lime", "Purple", "Orange", "Blue", "White")
    fun color(index: Int): Long = habitColors[((index % 5) + 5) % 5]

    // ---------- emblems ----------
    data class Emblem(val key: String, val label: String, val strokes: List<String>, val fills: List<String> = emptyList())

    private fun c(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    val emblems: List<Emblem> = listOf(
        Emblem("focus", "Focus", listOf(c(12f, 12f, 9f), c(12f, 12f, 5f)), listOf(c(12f, 12f, 1.3f))),
        Emblem("bolt", "Move", listOf("M13 2L5 13h7l-1 9 8-11h-7z")),
        Emblem("book", "Read", listOf("M3 5l9 2 9-2v14l-9 2-9-2zM12 7v14")),
        Emblem("nophone", "Screens", listOf("M7 3h10v18H7z", "M10.5 17.5h3M3 3l18 18")),
        Emblem("shot", "Shot", listOf("M5 4h14l-2 16H7zM6.2 9h11.6")),
        Emblem("cold", "Cold", listOf("M12 2v20M3.3 7l17.4 10M3.3 17l17.4-10M9 3.5l3 3 3-3M9 20.5l3-3 3 3")),
        Emblem("sleep", "Sleep", listOf("M19.5 14.5A8 8 0 1 1 9.5 4.5a7.2 7.2 0 0 0 10 10z")),
        Emblem("sun", "Wake", listOf(c(12f, 12f, 4f), "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1")),
        Emblem("water", "Water", listOf("M12 3l5.5 7.5a7 7 0 1 1-11 0z")),
        Emblem("lift", "Lift", listOf("M6 7v10M18 7v10M3 10v4M21 10v4M6 12h12")),
        Emblem("leaf", "Greens", listOf("M5 19C5 10 10 5 19 5c0 9-5 14-14 14zM5 19l8-8")),
        Emblem("cube", "Sugar", listOf("M12 3l8 4.5v9L12 21l-8-4.5v-9zM4 7.5l8 4.5 8-4.5M12 12v9")),
        Emblem("pen", "Write", listOf("M4 20l1-5L16 4l4 4L9 19zM13.5 6.5l4 4")),
        Emblem("heart", "Heart", listOf("M12 20L3.5 11.5A5.1 5.1 0 0 1 12 6A5.1 5.1 0 0 1 20.5 11.5Z")),
        Emblem("bed", "Rest", listOf("M3 5v15M3 15h18v5M21 15v-4H10v4M5.5 11h2")),
        Emblem("stretch", "Stretch", listOf("M3 12h18M7 8l-4 4 4 4M17 8l4 4-4 4")),
        Emblem("mountain", "Climb", listOf("M2 20L9 8l4 6 3-4 6 10z")),
        Emblem("flame", "Fire", listOf("M12 2c1 5 7 7 7 13a7 7 0 0 1-14 0c0-4 3-5 3-8 2 2 2 4 4 4 0-3-1-5 0-9z")),
        Emblem("clock", "Time", listOf(c(12f, 12f, 9f), "M12 7v5l3 3")),
        Emblem("bowl", "Eat clean", listOf("M3 11h18a9 9 0 0 1-18 0zM9 7V4M12 7V3M15 7V4")),
        Emblem("coffee", "Coffee", listOf("M4 8h12v7a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4zM16 10h2a2 2 0 0 1 0 4h-2M8 3v2M12 3v2")),
        Emblem("route", "Walk", listOf(c(6f, 18f, 2f), c(18f, 6f, 2f), "M8 18h6a3 3 0 0 0 0-6h-4a3 3 0 0 1 0-6h6")),
        Emblem("sprout", "Grow", listOf("M12 21V11M12 11C12 7 9 5 5 5c0 4 3 6 7 6zM12 13c0-4 3-6 7-6 0 4-3 6-7 6z")),
        Emblem("chat", "Talk", listOf("M4 4h16v12H10l-5 4v-4H4z")),
        Emblem("breathe", "Breathe", listOf("M3 8h11a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h8")),
        Emblem("money", "Save", listOf("M3 6h18v12H3z", c(12f, 12f, 2.5f))),
        Emblem("teeth", "Floss", listOf("M7 3C4 3 3 6 4 9l2 12h2l2-6h4l2 6h2l2-12c1-3 0-6-3-6-2 0-3 1-5 1S9 3 7 3z")),
        Emblem("squig", "Bitter", listOf("M3 10.2L6.6 6.6L10.2 10.2L13.8 6.6L21 13.8L17.4 17.4L13.8 13.8L10.2 17.4Z")),
    )

    fun emblem(key: String): Emblem = emblems.firstOrNull { it.key == key } ?: emblems.last()

    // ---------- Bitter wordmark (from the supplied logo SVG), viewBox 20 33 166 53 ----------
    const val LOGO_X = 20f
    const val LOGO_Y = 33f
    const val LOGO_W = 166f
    const val LOGO_H = 53f

    val logoLetters: List<String> = listOf(
        "M51.02,58.27c1.17-0.65,2.17-1.41,3-2.29c1.91-2.05,2.87-4.55,2.87-7.52c0-2.32-0.57-4.44-1.71-6.35c-1.14-1.91-2.79-3.44-4.95-4.58c-2.16-1.14-4.79-1.71-7.89-1.71H32.22h-2.39h-8.2v48.24h8.2h2.39h9.98c3.42,0,6.34-0.61,8.78-1.84c2.44-1.23,4.3-2.9,5.6-5.02c1.3-2.12,1.95-4.5,1.95-7.14c0-3.46-1.1-6.38-3.31-8.75C54.07,60.08,52.67,59.07,51.02,58.27z M40.28,44.16c1.78,0,3.21,0.44,4.3,1.33c1.09,0.89,1.64,2.22,1.64,4c0,1.82-0.55,3.18-1.64,4.07c-1.09,0.89-2.53,1.33-4.3,1.33h-8.06V44.16H40.28z M46.09,73.88c-1.19,1.23-2.82,1.84-4.92,1.84h-8.95V63.02h8.95c1.41,0,2.61,0.27,3.59,0.82c0.98,0.55,1.74,1.3,2.29,2.25c0.55,0.96,0.82,2.05,0.82,3.28C47.87,71.15,47.28,72.65,46.09,73.88z",
        "M69.98,34.8c-1.64,0-3,0.55-4.07,1.64c-1.07,1.09-1.61,2.46-1.61,4.1c0,1.6,0.54,2.95,1.61,4.07c1.07,1.12,2.43,1.67,4.07,1.67c1.64,0,2.98-0.56,4.03-1.67c1.05-1.12,1.57-2.47,1.57-4.07c0-1.64-0.52-3.01-1.57-4.1C72.96,35.35,71.62,34.8,69.98,34.8z",
        "M64.72,50.86h10.45v33.21h-10.45z",
        "M151.04,52.22c-2.51-1.41-5.4-2.12-8.68-2.12c-3.37,0-6.4,0.75-9.09,2.25c-2.69,1.5-4.82,3.56-6.39,6.18c-1.57,2.62-2.36,5.59-2.36,8.92c0,3.33,0.8,6.31,2.39,8.95c1.59,2.64,3.79,4.7,6.59,6.18c2.8,1.48,6,2.22,9.6,2.22c2.78,0,5.33-0.48,7.65-1.43c2.32-0.96,4.3-2.39,5.94-4.3l-5.74-5.74c-1,1.14-2.16,1.99-3.48,2.56c-1.32,0.57-2.8,0.85-4.44,0.85c-1.78,0-3.31-0.38-4.61-1.13c-1.3-0.75-2.3-1.82-3.01-3.21c-0.23-0.46-0.41-0.96-0.57-1.48l23.77-0.06c0.18-0.86,0.31-1.63,0.38-2.29c0.07-0.66,0.1-1.29,0.1-1.88c0-3.23-0.72-6.1-2.15-8.61C155.52,55.6,153.55,53.64,151.04,52.22z M138.17,59.23c1.21-0.75,2.63-1.13,4.27-1.13c1.55,0,2.86,0.33,3.93,0.99c1.07,0.66,1.88,1.62,2.43,2.87c0.23,0.52,0.4,1.12,0.54,1.76l-14.47,0.04c0.14-0.48,0.3-0.93,0.5-1.35C136.02,61.04,136.96,59.98,138.17,59.23z",
        "M178.97,50.86c-8.49,0-15.37,6.88-15.37,15.37v17.84h10.45V67.75c0-3.48,2.82-6.3,6.3-6.3h4.47V50.86H178.97z",
        "M94.65,69.48V58.31l-4.19,3.74l-6.21-5.64v13.07c0,7.91,6.44,14.35,14.35,14.35h0.8v-10.4h-0.8C96.42,73.43,94.65,71.66,94.65,69.48z",
        "M94.65,41.42L94.65,37.12L84.24,37.12L84.24,39.45L90.46,45.17Z",
        "M114,40.48L114,37.12L103.59,37.12L103.59,40.56L108.76,45.17Z",
        "M114,69.48V57.37l-5.24,4.68l-5.16-4.61v12.04c0,7.91,6.44,14.35,14.35,14.35h0.8v-10.4h-0.8C115.77,73.43,114,71.66,114,69.48z",
    )
    const val logoSquiggle =
        "M117.91,38.99L108.76,47.17L99.61,38.99L90.46,47.17L82.46,39.8L76.05,46.97L90.46,60.06L99.61,51.88L108.76,60.06L117.91,51.88L123.85,57.2L130.26,50.03Z"

    // ---------- the week squiggle ----------
    // Day k is one whole rectangle at 45 degrees, in grid units u:
    //   L(2k,1)  T(2k+1,0)  R(2k+3,2)  B(2k+2,3)
    // Seven sit edge to edge (15u x 3u). With squares of size s on pitch p, u = p/2 and each
    // rectangle shares its centre with the square it replaces.

    /** Corners of day k's rectangle, in units: L, T, R, B. */
    fun bar(k: Int): List<Pair<Float, Float>> = listOf(
        (2f * k) to 1f, (2f * k + 1) to 0f, (2f * k + 3) to 2f, (2f * k + 2) to 3f,
    )

    /** Outline of rectangles k..m joined, in units. */
    fun run(k: Int, m: Int): List<Pair<Float, Float>> {
        val pts = mutableListOf<Pair<Float, Float>>()
        for (j in k..m) { pts.add((2f * j) to 1f); pts.add((2f * j + 1) to 0f) }
        pts.add((2f * m + 3) to 2f); pts.add((2f * m + 2) to 3f)
        for (j in m - 1 downTo k) { pts.add((2f * j + 3) to 2f); pts.add((2f * j + 2) to 3f) }
        return pts
    }

    /** Consecutive runs of days where pick(day) is true. */
    fun runs(pick: List<Boolean>): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (i in 0..pick.size) {
            val on = i < pick.size && pick[i]
            if (on && start < 0) start = i
            if (!on && start >= 0) { out.add(start until i); start = -1 }
        }
        return out
    }

    val principles = listOf(
        "Be comfortable with the uncomfortable.",
        "Grow up.",
        "Hard now, easy later.",
        "Do the work outside the arena.",
        "Nobody claps for the early start.",
        "Bitter is the point.",
        "Show up. Then show up again.",
    )
}
