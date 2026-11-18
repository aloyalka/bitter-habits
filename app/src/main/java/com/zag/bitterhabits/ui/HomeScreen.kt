package com.zag.bitterhabits.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.zag.bitterhabits.data.AppData
import com.zag.bitterhabits.data.DayState
import com.zag.bitterhabits.data.Freq
import com.zag.bitterhabits.data.Habit
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.data.TapOutcome
import com.zag.bitterhabits.data.TapResult
import com.zag.bitterhabits.data.Ticks
import com.zag.bitterhabits.data.avgBitterness
import com.zag.bitterhabits.data.dayStreak
import com.zag.bitterhabits.data.doneInMonth
import com.zag.bitterhabits.data.doneInWeek
import com.zag.bitterhabits.data.dueInMonth
import com.zag.bitterhabits.data.extra
import com.zag.bitterhabits.data.isPerfect
import com.zag.bitterhabits.data.perfectWeeksThisYear
import com.zag.bitterhabits.data.required
import com.zag.bitterhabits.data.state
import com.zag.bitterhabits.data.weekStart
import com.zag.bitterhabits.data.weekStates
import com.zag.bitterhabits.data.weekStreak
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Week strip size on the home screen.
private val CELL = 20.dp
private val PITCH = 24.dp
private val PAD = 9.dp

// Month calendar size.
private val MCELL = 34.dp
private val MPITCH = 42.dp
private val MPAD = 14.dp

private val LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

private sealed class Overlay {
    data class DayMenu(val habitId: String, val day: LocalDate) : Overlay()
    data class Rate(val habitId: String, val day: LocalDate) : Overlay()
    data class Untick(val habitId: String, val day: LocalDate) : Overlay()
}

private data class Celebration(val habitId: String, val weekStart: LocalDate)

private val GLYPHS = mapOf(
    "plus" to "M12 5v14M5 12h14",
    "sliders" to "M4 7h9M17 7h3M4 17h3M11 17h9M13 5h4v4h-4zM7 15h4v4H7z",
    "back" to "M15 5l-7 7 7 7",
    "left" to "M14 6l-6 6 6 6",
    "right" to "M10 6l6 6-6 6",
    "up" to "M6 14l6-6 6 6",
    "down" to "M6 10l6 6 6-6",
)

@Composable
fun Glyph(name: String, color: Color = C.tx, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val sc = this.size.width / 24f
        scale(sc, sc, pivot = Offset.Zero) {
            drawPath(parsedPath(GLYPHS.getValue(name)), color, style = Stroke(1.8f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
        }
    }
}

@Composable
fun HomeScreen(
    data: AppData,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (String) -> Unit,
    pendingUntick: Pair<String, Long>?,
    onPendingHandled: () -> Unit,
) {
    val ctx = LocalContext.current
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }

    var mode by rememberSaveable { mutableIntStateOf(0) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var monthHabit by rememberSaveable { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(celebration) {
        if (celebration != null) {
            anim.snapTo(0f)
            anim.animateTo(3.4f, tween(3400, easing = LinearEasing))
            celebration = null
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(2600); toast = null }
    }
    LaunchedEffect(pendingUntick) {
        val p = pendingUntick
        if (p != null) {
            overlay = Overlay.Untick(p.first, LocalDate.ofEpochDay(p.second))
            onPendingHandled()
        }
    }

    fun handle(r: TapResult, h: Habit, day: LocalDate) {
        when (r.outcome) {
            TapOutcome.IGNORED_FUTURE -> toast = "That day hasn't happened yet."
            TapOutcome.NEEDS_CONFIRM -> overlay = Overlay.Untick(h.id, day)
            TapOutcome.UNTICKED -> {}
            TapOutcome.TICKED -> {
                if (r.becamePerfect) celebration = Celebration(h.id, day.weekStart())
                when {
                    r.wentOver -> toast = "+1. Above and beyond. Noted."
                    r.streakMilestone == 7 -> toast = "A week in. Getting less bitter."
                    r.streakMilestone == 30 -> toast = "Thirty days. This is just who you are now."
                    r.streakMilestone == 100 -> toast = "A hundred. Grown up."
                }
                if (h.rateOn) overlay = Overlay.Rate(h.id, day)
            }
        }
    }

    val tapDay: (Habit, LocalDate) -> Unit = { h, d -> handle(Ticks.tap(ctx, h.id, d), h, d) }
    val longDay: (Habit, LocalDate) -> Unit = { h, d -> overlay = Overlay.DayMenu(h.id, d) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo(26.dp)
                Spacer(Modifier.weight(1f))
                IconTile(Modifier.tap(onAdd), size = 40) { Glyph("plus") }
                Spacer(Modifier.width(6.dp))
                IconTile(Modifier.tap(onSettings), size = 40) { Glyph("sliders") }
            }
            val dateLine = today.format(DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.ENGLISH)).uppercase()
            Text(dateLine, style = ts(12.sp, FontWeight.Medium, C.tx2, 1.6.sp), modifier = Modifier.padding(start = 20.dp, top = 18.dp))
            Text(
                rememberPrinciple(today.dayOfYear),
                style = ts(23.sp, FontWeight.Medium).copy(lineHeight = 27.sp),
                modifier = Modifier.padding(start = 20.dp, end = 60.dp, top = 4.dp, bottom = 16.dp),
            )
            Segmented(listOf("Week", "Month"), mode, { mode = it }, Modifier.padding(horizontal = 20.dp))

            if (mode == 0) {
                val ws = today.weekStart().plusWeeks(weekOffset.toLong())
                WeekContent(
                    data, ws, today, weekOffset,
                    onPrev = { weekOffset -= 1 }, onNext = { if (weekOffset < 0) weekOffset += 1 },
                    celebration = celebration, animT = anim.value,
                    onTap = tapDay, onLong = longDay, onEdit = onEdit, onAdd = onAdd,
                )
            } else {
                val habits = data.active
                val selected = habits.firstOrNull { it.id == monthHabit } ?: habits.firstOrNull()
                MonthContent(
                    data, selected, habits, today, today.withDayOfMonth(1).plusMonths(monthOffset.toLong()), monthOffset,
                    onSelect = { monthHabit = it }, onPrev = { monthOffset -= 1 }, onNext = { if (monthOffset < 0) monthOffset += 1 },
                    celebration = celebration, animT = anim.value, onTap = tapDay, onLong = longDay, onAdd = onAdd,
                )
            }
        }

        toast?.let {
            Banner(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp, start = 20.dp, end = 20.dp))
        }

        when (val o = overlay) {
            is Overlay.DayMenu -> DayMenuSheet(data, o, today, onClose = { overlay = null }, onRate = { overlay = Overlay.Rate(o.habitId, o.day) },
                onCelebrate = { celebration = Celebration(o.habitId, o.day.weekStart()) }, onResult = { r, h -> handle(r, h, o.day) })
            is Overlay.Rate -> RateSheet(data, o.habitId, o.day, onClose = { overlay = null })
            is Overlay.Untick -> UntickSheet(data, o.habitId, o.day, today, onClose = { overlay = null })
            null -> {}
        }
    }
}

// ---------------- week ----------------

@Composable
private fun ColumnScope.WeekContent(
    data: AppData, ws: LocalDate, today: LocalDate, offset: Int,
    onPrev: () -> Unit, onNext: () -> Unit,
    celebration: Celebration?, animT: Float,
    onTap: (Habit, LocalDate) -> Unit, onLong: (Habit, LocalDate) -> Unit, onEdit: (String) -> Unit, onAdd: () -> Unit,
) {
    val range = rangeLabel(ws)
    val title = when (offset) { 0 -> "This week"; -1 -> "Last week"; else -> null }
    NavRow(title, range, canNext = offset < 0, onPrev = onPrev, onNext = onNext)

    val habits = data.active
    if (habits.isEmpty()) {
        EmptyState(onAdd); return
    }
    DayHeaderBox(ws, today, CELL, PITCH, PAD, Modifier.align(Alignment.End).padding(end = 8.dp))
    habits.forEach { h ->
        val animating = celebration != null && celebration.habitId == h.id && celebration.weekStart == ws
        HabitRow(data, h, ws, today, if (animating) animT else null, onTap, onLong, onEdit)
    }
    if (offset == 0) {
        val due = habits.filter { data.state(it, today, today) != DayState.REST }
        val doneToday = due.count { data.state(it, today, today) == DayState.DONE }
        Row(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(C.surf)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Label("Today")
                Text("$doneToday of ${due.size} done", style = ts(20.sp, FontWeight.Medium))
            }
            Text(
                if (due.isNotEmpty() && doneToday == due.size) "All done. Go recover." else "Tap a day to tick it",
                style = ts(13.5.sp, color = C.tx2),
            )
        }
    }
}

@Composable
private fun DayHeaderBox(ws: LocalDate, today: LocalDate, cell: Dp, pitch: Dp, pad: Dp, modifier: Modifier = Modifier, showDates: Boolean = true) {
    val measurer = rememberTextMeasurer()
    val width = pad + cell / 2 + pitch * 6.75f + 2.dp
    Canvas(modifier.size(width, if (showDates) 40.dp else 22.dp)) {
        for (k in 0..6) {
            val d = ws.plusDays(k.toLong())
            val cx = pad.toPx() + k * pitch.toPx() + cell.toPx() / 2f
            val isToday = showDates && d == today
            if (isToday) drawRoundRect(C.tx, Offset(cx - 12.dp.toPx(), 0f), Size(24.dp.toPx(), 38.dp.toPx()), CornerRadius(8.dp.toPx()))
            val l = measurer.measure(LETTERS[k], ts(12.5.sp, FontWeight.SemiBold))
            drawText(l, color = if (isToday) C.bg else C.tx2, topLeft = Offset(cx - l.size.width / 2f, 3.dp.toPx()))
            if (showDates) {
                val n = measurer.measure(d.dayOfMonth.toString(), ts(10.5.sp))
                drawText(n, color = if (isToday) C.bg else C.tx3, topLeft = Offset(cx - n.size.width / 2f, 20.dp.toPx()))
            }
        }
    }
}

@Composable
private fun HabitRow(
    data: AppData, h: Habit, ws: LocalDate, today: LocalDate, morphT: Float?,
    onTap: (Habit, LocalDate) -> Unit, onLong: (Habit, LocalDate) -> Unit, onEdit: (String) -> Unit,
) {
    val color = habitColor(h.color)
    val states = data.weekStates(h, ws, today)
    val perfect = data.isPerfect(h, ws, today)
    val showText = if (morphT == null) 0f else {
        val inT = ((morphT - 0.9f) / 0.35f).coerceIn(0f, 1f)
        val outT = ((morphT - 2.5f) / 0.4f).coerceIn(0f, 1f)
        (inT - outT).coerceIn(0f, 1f)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
    Row(
        Modifier.fillMaxWidth().height(66.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(Modifier.tap { onEdit(h.id) }, size = 34) { EmblemIcon(h.emblem, color, 18.dp) }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).tap { onEdit(h.id) }) {
            Column(Modifier.alpha(1f - showText)) {
                Text(h.name, style = ts(14.sp).copy(lineHeight = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle(data, h, ws, today, color), style = ts(11.5.sp, color = C.tx2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showText > 0f) {
                Text("Perfect week.", style = ts(16.5.sp, FontWeight.SemiBold, color), modifier = Modifier.alpha(showText).align(Alignment.CenterStart))
            }
        }
        WeekStrip(
            states, perfect, color,
            morphT = morphT, cell = CELL, pitch = PITCH, startPad = PAD, height = 66.dp,
            onTap = { k -> onTap(h, ws.plusDays(k.toLong())) },
            onLongPress = { k -> onLong(h, ws.plusDays(k.toLong())) },
        )
    }
}

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DAY_PLURAL = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

fun restLabel(rest: Set<Int>): String = when {
    rest.isEmpty() -> "Every day"
    rest.size == 1 -> "${DAY_PLURAL[rest.first() - 1]} off"
    7 - rest.size <= 4 -> (1..7).filter { it !in rest }.joinToString(", ") { DAY_NAMES[it - 1] }
    else -> rest.sorted().joinToString(", ") { DAY_NAMES[it - 1] } + " off"
}

fun goalLabel(h: Habit): String = when (h.freq) {
    Freq.WEEKLY -> "${h.weeklyTarget} times a week"
    Freq.DAILY -> restLabel(h.restDays)
}

private fun subtitle(data: AppData, h: Habit, ws: LocalDate, today: LocalDate, color: Color): AnnotatedString = buildAnnotatedString {
    val current = ws == today.weekStart()
    val done = data.doneInWeek(h, ws)
    when (h.freq) {
        Freq.DAILY -> {
            if (current) {
                val base = if (h.restDays.isEmpty()) "Every day" else restLabel(h.restDays)
                val streak = data.dayStreak(h, today)
                if (streak >= 2) {
                    append("$base · ")
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) { append("$streak-day streak") }
                } else append(base)
            } else {
                append("$done of ${data.required(h, ws)}")
                if (h.restDays.isNotEmpty()) append(" · ${restLabel(h.restDays)}")
                val x = data.extra(h, ws)
                if (x > 0) { append(" · "); withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append("+$x") } }
            }
        }
        Freq.WEEKLY -> {
            append("${h.weeklyTarget} a week · ")
            if (done >= h.weeklyTarget) {
                val x = done - h.weeklyTarget
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) { append(if (x > 0) "Goal met · +$x" else "Goal met") }
            } else append("$done of ${h.weeklyTarget}")
        }
    }
}

private fun rangeLabel(ws: LocalDate): String {
    val we = ws.plusDays(6)
    val f = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    return if (ws.month == we.month) "${ws.dayOfMonth}–${we.format(f)}" else "${ws.format(f)} – ${we.format(f)}"
}

@Composable
private fun NavRow(title: String?, detail: String, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).tap(onPrev), contentAlignment = Alignment.Center) { Glyph("left") }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            if (title != null) {
                Text(title, style = ts(14.5.sp, FontWeight.Medium))
                Text(" · ", style = ts(14.5.sp, color = C.tx2))
            }
            Text(detail, style = ts(14.5.sp, color = C.tx2))
        }
        Box(Modifier.size(40.dp).tap { if (canNext) onNext() }, contentAlignment = Alignment.Center) {
            Glyph("right", if (canNext) C.tx else C.chip)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing to track yet.", style = ts(20.sp, FontWeight.Medium))
        Text("Add one hard thing. Just one.", style = ts(15.sp, color = C.tx2), modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        BigButton("Add a habit", Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

// ---------------- month ----------------

@Composable
private fun MonthContent(
    data: AppData, h: Habit?, habits: List<Habit>, today: LocalDate, month: LocalDate, offset: Int,
    onSelect: (String) -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    celebration: Celebration?, animT: Float,
    onTap: (Habit, LocalDate) -> Unit, onLong: (Habit, LocalDate) -> Unit, onAdd: () -> Unit,
) {
    if (h == null) { Spacer(Modifier.height(12.dp)); EmptyState(onAdd); return }
    val color = habitColor(h.color)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        habits.forEach { x ->
            val on = x.id == h.id
            val xc = habitColor(x.color)
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) xc else C.surf)
                    .tap { onSelect(x.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmblemIcon(x.emblem, if (on) C.bg else xc, 15.dp)
                Spacer(Modifier.width(6.dp))
                Text(x.name, style = ts(13.5.sp, if (on) FontWeight.Medium else FontWeight.Normal, if (on) C.bg else Color(0xFFC9D1C6)))
            }
        }
    }
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.year
    NavRow(monthName, goalLabel(h), canNext = offset < 0, onPrev = onPrev, onNext = onNext)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        DayHeaderBox(month.weekStart(), today, MCELL, MPITCH, MPAD, showDates = false)
        var ws = month.weekStart()
        val last = month.withDayOfMonth(month.lengthOfMonth())
        while (!ws.isAfter(last)) {
            val week = ws
            val perfect = data.isPerfect(h, week, today)
            val full = data.weekStates(h, week, today)
            val states = if (perfect) full else full.mapIndexed { k, s -> if (week.plusDays(k.toLong()).month == month.month) s else DayState.NONE }
            val nums = (0..6).map { k -> val d = week.plusDays(k.toLong()); if (d.month == month.month) d.dayOfMonth else null }
            val animating = celebration != null && celebration.habitId == h.id && celebration.weekStart == week
            WeekStrip(
                states, perfect, color,
                morphT = if (animating) animT else null,
                cell = MCELL, pitch = MPITCH, startPad = MPAD, height = if (perfect || animating) 72.dp else 48.dp,
                numbers = nums,
                onTap = { k -> if (states[k] != DayState.NONE) onTap(h, week.plusDays(k.toLong())) },
                onLongPress = { k -> if (states[k] != DayState.NONE) onLong(h, week.plusDays(k.toLong())) },
            )
            ws = ws.plusWeeks(1)
        }
    }
    Legend(color)
    Stats(data, h, today, month)
}

@Composable
private fun Legend(color: Color) {
    Row(Modifier.padding(start = 20.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(DayState.DONE to "Done", DayState.REST to "Rest", DayState.OPEN to "Not done", DayState.FUTURE to "Ahead").forEach { (s, l) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(14.dp)) { drawCell(s, 0f, 0f, size.width, size.width * 0.27f, color, 1.3.dp.toPx(), false) }
                Spacer(Modifier.width(6.dp))
                Text(l, style = ts(12.5.sp, color = C.tx2))
            }
        }
    }
}

@Composable
private fun Stats(data: AppData, h: Habit, today: LocalDate, month: LocalDate) {
    val ws = today.weekStart()
    val avg = data.avgBitterness(h, ws)
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Perfect weeks", data.perfectWeeksThisYear(h, today).toString(), "this year", Modifier.weight(1f))
            if (h.freq == Freq.DAILY) StatCard("Current streak", data.dayStreak(h, today).toString(), "days", Modifier.weight(1f))
            else StatCard("Weeks in a row", data.weekStreak(h, today).toString(), "weeks", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (h.freq == Freq.DAILY) StatCard("This month", data.doneInMonth(h, month, today).toString(), "of ${data.dueInMonth(h, month, today)}", Modifier.weight(1f))
            else StatCard("This month", data.doneInMonth(h, month, today).toString(), "sessions", Modifier.weight(1f))
            if (h.rateOn) StatCard("Avg. bitterness", avg?.let { String.format(Locale.ENGLISH, "%.1f", it) } ?: "–", "this week", Modifier.weight(1f))
            else StatCard("Avg. bitterness", "–", "rating off", Modifier.weight(1f))
        }
        if (h.rateOn) Trend(data, h, today)
    }
}

@Composable
private fun Trend(data: AppData, h: Habit, today: LocalDate) {
    val weeks = (7 downTo 0).map { today.weekStart().minusWeeks(it.toLong()) }
    val vals = weeks.map { data.avgBitterness(h, it) }
    val known = vals.filterNotNull()
    val line = when {
        known.size < 2 -> "Rate a few more days to see the trend."
        known.last() < known.first() - 0.2 -> "Yes. Down from ${fmt(known.first())} to ${fmt(known.last())} in ${vals.size} weeks."
        known.last() > known.first() + 0.2 -> "Harder lately. ${fmt(known.last())}, up from ${fmt(known.first())}. Worth a look."
        else -> "Not yet. Still around ${fmt(known.last())}. Keep going."
    }
    val color = habitColor(h.color)
    val measurer = rememberTextMeasurer()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surf).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Label("Is it getting easier?")
        Text(line, style = ts(15.sp), modifier = Modifier.padding(top = 3.dp, bottom = 10.dp))
        Canvas(Modifier.fillMaxWidth().height(76.dp)) {
            val bw = 26.dp.toPx(); val gap = (size.width - bw * 8) / 7f
            vals.forEachIndexed { i, v ->
                val x = i * (bw + gap)
                if (v != null) {
                    val bh = (v / 5.0 * 56.dp.toPx()).toFloat()
                    val c = if (i == 7) color else Color(0xFF2E3A31)
                    drawRoundRect(c, Offset(x, size.height - bh), Size(bw, bh), CornerRadius(3.dp.toPx()))
                    val l = measurer.measure(fmt(v), ts(10.5.sp))
                    drawText(l, color = if (i == 7) color else C.tx3, topLeft = Offset(x + bw / 2 - l.size.width / 2f, size.height - bh - l.size.height - 2.dp.toPx()))
                }
            }
        }
    }
}

private fun fmt(v: Double) = String.format(Locale.ENGLISH, "%.1f", v)

// ---------------- sheets ----------------

private fun dayTitle(d: LocalDate) = d.format(DateTimeFormatter.ofPattern("EEEE d", Locale.ENGLISH))

@Composable
private fun DayMenuSheet(
    data: AppData, o: Overlay.DayMenu, today: LocalDate,
    onClose: () -> Unit, onRate: () -> Unit, onCelebrate: () -> Unit, onResult: (TapResult, Habit) -> Unit,
) {
    val ctx = LocalContext.current
    val h = data.habit(o.habitId) ?: run { onClose(); return }
    val st = data.state(h, o.day, today)
    val e = o.day.toEpochDay()
    Sheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemIcon(h.emblem, habitColor(h.color), 16.dp)
            Spacer(Modifier.width(8.dp))
            Text("${h.name} · ${dayTitle(o.day)}", style = ts(14.sp, color = C.tx2))
        }
        Spacer(Modifier.height(8.dp))
        if (st != DayState.DONE && !o.day.isAfter(today)) SheetOption("Mark done") {
            onClose(); onResult(Ticks.tap(ctx, h.id, o.day), h)
        }
        if (st == DayState.DONE) SheetOption("Untick") {
            onClose(); onResult(Ticks.tap(ctx, h.id, o.day), h)
        }
        if (e !in data.restSet(h.id)) SheetOption("Mark as rest day") {
            val became = Ticks.markRest(ctx, h.id, o.day)
            onClose(); if (became) onCelebrate()
        }
        if (st == DayState.DONE && h.rateOn) SheetOption("Rate bitterness") { onRate() }
        if (e in data.restSet(h.id)) SheetOption("Clear") { Ticks.clear(ctx, h.id, o.day); onClose() }
        SheetOption("Cancel", C.tx2) { onClose() }
    }
}

private val RATING_WORDS = listOf("Barely", "Mild", "Bitter", "Very bitter", "Brutal")

@Composable
private fun RateSheet(data: AppData, habitId: String, day: LocalDate, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val h = data.habit(habitId) ?: run { onClose(); return }
    val color = habitColor(h.color)
    var value by remember { mutableIntStateOf(data.ratings[h.id]?.get(day.toEpochDay()) ?: 0) }
    Sheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemIcon(h.emblem, color, 16.dp)
            Spacer(Modifier.width(8.dp))
            Text("${h.name} · done for ${if (day == LocalDate.now()) "today" else dayTitle(day)}", style = ts(14.sp, color = C.tx2))
        }
        Text("How bitter was it?", style = ts(28.sp, FontWeight.SemiBold), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Text("Optional. Rate it and watch it get easier.", style = ts(14.5.sp, color = C.tx2), modifier = Modifier.padding(bottom = 22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 1..5) {
                Column(Modifier.tap { value = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    SquareCheck(i <= value, color, 44)
                    Text("$i", style = ts(14.sp, FontWeight.Medium, if (i <= value) C.tx else C.tx3), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Barely", style = ts(12.5.sp, color = C.tx3)); Text("Brutal", style = ts(12.5.sp, color = C.tx3))
        }
        Text(
            if (value == 0) " " else "$value · ${RATING_WORDS[value - 1]}",
            style = ts(20.sp, FontWeight.Medium, color),
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Skip", Modifier.weight(1f), ghost = true) { onClose() }
            BigButton("Save", Modifier.weight(2f)) {
                if (value > 0) Store.update(ctx) { it.setRating(h.id, day.toEpochDay(), value) }
                onClose()
            }
        }
    }
}

@Composable
private fun UntickSheet(data: AppData, habitId: String, day: LocalDate, today: LocalDate, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val h = data.habit(habitId) ?: run { onClose(); return }
    val color = habitColor(h.color)
    val ws = day.weekStart()
    val after = data.setDone(h.id, day.toEpochDay(), false)
    val doneAfter = after.doneInWeek(h, ws)
    val need = data.required(h, ws)
    Sheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemIcon(h.emblem, color, 16.dp)
            Spacer(Modifier.width(8.dp))
            Text("${h.name} · ${dayTitle(day)}", style = ts(14.sp, color = C.tx2))
        }
        Text("Untick ${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}?", style = ts(26.sp, FontWeight.SemiBold), modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        Text(
            "This drops you to $doneAfter of $need and breaks the perfect week until it's complete again.",
            style = ts(15.sp, color = C.tx2).copy(lineHeight = 21.sp), modifier = Modifier.padding(bottom = 16.dp),
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            WeekStrip(data.weekStates(h, ws, today), true, color, cell = 14.dp, pitch = 17.dp, startPad = 6.dp, height = 40.dp)
            Text("→", style = ts(20.sp, color = C.tx3), modifier = Modifier.padding(horizontal = 8.dp))
            WeekStrip(after.weekStates(h, ws, today), false, color, cell = 14.dp, pitch = 17.dp, startPad = 6.dp, height = 40.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Keep it", Modifier.weight(1f), ghost = true) { onClose() }
            BigButton("Untick", Modifier.weight(1f), color = C.tx) {
                Ticks.tap(ctx, h.id, day, confirmed = true); onClose()
            }
        }
    }
}
