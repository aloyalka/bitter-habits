package com.zag.bitterhabits.data

import android.content.Context
import java.time.LocalDate

enum class DayState { DONE, REST, OPEN, TODAY, FUTURE, NONE }

fun LocalDate.weekStart(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

fun AppData.doneInWeek(h: Habit, ws: LocalDate): Int {
    val s = doneSet(h.id)
    return (0..6).count { ws.plusDays(it.toLong()).toEpochDay() in s }
}

/**
 * What a day looks like.
 * - Planned rest days (every-day habits) show for past weeks and the current week only, so future weeks stay clean.
 * - Weekly goals: once the target is hit, every other day that week becomes rest.
 */
fun AppData.state(h: Habit, d: LocalDate, today: LocalDate): DayState {
    val e = d.toEpochDay()
    if (e in doneSet(h.id)) return DayState.DONE
    if (e in restSet(h.id)) return DayState.REST
    val ws = d.weekStart()
    val notFutureWeek = !ws.isAfter(today.weekStart())
    if (notFutureWeek && h.freq == Freq.DAILY && d.dayOfWeek.value in h.restDays) return DayState.REST
    if (notFutureWeek && h.freq == Freq.WEEKLY && doneInWeek(h, ws) >= h.weeklyTarget) return DayState.REST
    return when {
        d.isAfter(today) -> DayState.FUTURE
        d == today -> DayState.TODAY
        else -> DayState.OPEN
    }
}

fun AppData.weekStates(h: Habit, ws: LocalDate, today: LocalDate): List<DayState> =
    (0..6).map { state(h, ws.plusDays(it.toLong()), today) }

/** Every day of the Monday-to-Sunday week is done or rest, with at least one done. */
fun AppData.isPerfect(h: Habit, ws: LocalDate, today: LocalDate): Boolean {
    if (ws.isAfter(today.weekStart())) return false
    val st = weekStates(h, ws, today)
    return st.all { it == DayState.DONE || it == DayState.REST } && st.any { it == DayState.DONE }
}

/** How many sessions the week asks for. */
fun AppData.required(h: Habit, ws: LocalDate): Int = when (h.freq) {
    Freq.WEEKLY -> h.weeklyTarget
    Freq.DAILY -> {
        val manual = restSet(h.id)
        (0..6).count { i ->
            val d = ws.plusDays(i.toLong())
            d.dayOfWeek.value !in h.restDays && d.toEpochDay() !in manual
        }
    }
}

fun AppData.extra(h: Habit, ws: LocalDate): Int = (doneInWeek(h, ws) - required(h, ws)).coerceAtLeast(0)

/** Days in a row (daily habits). Rest days don't break it; today doesn't count against you until it's over. */
fun AppData.dayStreak(h: Habit, today: LocalDate): Int {
    var d = today
    var n = 0
    if (state(h, d, today) == DayState.TODAY) d = d.minusDays(1)
    var guard = 0
    while (guard < 3650) {
        when (state(h, d, today)) {
            DayState.DONE -> n++
            DayState.REST -> {}
            else -> return n
        }
        d = d.minusDays(1); guard++
    }
    return n
}

/** Weeks in a row where the goal was met (weekly habits) or the week was perfect (daily habits). */
fun AppData.weekStreak(h: Habit, today: LocalDate): Int {
    var ws = today.weekStart()
    var n = 0
    if (!weekMet(h, ws, today)) ws = ws.minusWeeks(1)
    var guard = 0
    while (guard < 520 && weekMet(h, ws, today)) { n++; ws = ws.minusWeeks(1); guard++ }
    return n
}

fun AppData.weekMet(h: Habit, ws: LocalDate, today: LocalDate): Boolean = when (h.freq) {
    Freq.WEEKLY -> doneInWeek(h, ws) >= h.weeklyTarget
    Freq.DAILY -> isPerfect(h, ws, today)
}

fun AppData.perfectWeeksThisYear(h: Habit, today: LocalDate): Int {
    var ws = LocalDate.of(today.year, 1, 1).weekStart()
    if (ws.year < today.year) ws = ws.plusWeeks(1)
    var n = 0
    while (!ws.isAfter(today.weekStart())) { if (isPerfect(h, ws, today)) n++; ws = ws.plusWeeks(1) }
    return n
}

fun AppData.doneInMonth(h: Habit, month: LocalDate, today: LocalDate): Int {
    val first = month.withDayOfMonth(1)
    val s = doneSet(h.id)
    return (0 until first.lengthOfMonth()).count { i ->
        val d = first.plusDays(i.toLong()); !d.isAfter(today) && d.toEpochDay() in s
    }
}

/** For every-day habits: days so far this month that were not rest days. */
fun AppData.dueInMonth(h: Habit, month: LocalDate, today: LocalDate): Int {
    val first = month.withDayOfMonth(1)
    return (0 until first.lengthOfMonth()).count { i ->
        val d = first.plusDays(i.toLong())
        !d.isAfter(today) && state(h, d, today) != DayState.REST
    }
}

fun AppData.avgBitterness(h: Habit, ws: LocalDate): Double? {
    val m = ratings[h.id] ?: return null
    val vals = (0..6).mapNotNull { m[ws.plusDays(it.toLong()).toEpochDay()] }
    return if (vals.isEmpty()) null else vals.average()
}

/** True when unticking this done day would take away a perfect week. */
fun AppData.untickBreaksPerfect(h: Habit, day: LocalDate, today: LocalDate): Boolean {
    val e = day.toEpochDay()
    if (e !in doneSet(h.id)) return false
    val ws = day.weekStart()
    return isPerfect(h, ws, today) && !setDone(h.id, e, false).isPerfect(h, ws, today)
}

// ---------- ticking, shared by the app and the widget ----------

enum class TapOutcome { IGNORED_FUTURE, TICKED, UNTICKED, NEEDS_CONFIRM }

data class TapResult(
    val outcome: TapOutcome,
    val becamePerfect: Boolean = false,
    val wentOver: Boolean = false,
    val streakMilestone: Int = 0,
)

object Ticks {
    fun tap(ctx: Context, habitId: String, day: LocalDate, confirmed: Boolean = false): TapResult {
        val today = LocalDate.now()
        val before = Store.current(ctx)
        val h = before.habit(habitId) ?: return TapResult(TapOutcome.IGNORED_FUTURE)
        if (day.isAfter(today)) return TapResult(TapOutcome.IGNORED_FUTURE)
        val e = day.toEpochDay()
        val ws = day.weekStart()
        val wasPerfect = before.isPerfect(h, ws, today)
        if (e in before.doneSet(h.id)) {
            val wouldBreak = wasPerfect && !before.setDone(h.id, e, false).isPerfect(h, ws, today)
            if (wouldBreak && !confirmed) return TapResult(TapOutcome.NEEDS_CONFIRM)
            Store.update(ctx) { it.setDone(h.id, e, false).setRating(h.id, e, null) }
            return TapResult(TapOutcome.UNTICKED)
        }
        val extraBefore = before.extra(h, ws)
        val streakBefore = before.dayStreak(h, today)
        Store.update(ctx) { it.setRest(h.id, e, false).setDone(h.id, e, true) }
        val after = Store.current(ctx)
        val streakAfter = if (h.freq == Freq.DAILY) after.dayStreak(h, today) else 0
        val milestone = listOf(7, 30, 100).firstOrNull { streakBefore < it && streakAfter >= it } ?: 0
        return TapResult(
            outcome = TapOutcome.TICKED,
            becamePerfect = !wasPerfect && after.isPerfect(h, ws, today),
            wentOver = wasPerfect && after.extra(h, ws) > extraBefore,
            streakMilestone = milestone,
        )
    }

    fun markRest(ctx: Context, habitId: String, day: LocalDate): Boolean {
        val today = LocalDate.now()
        val before = Store.current(ctx)
        val h = before.habit(habitId) ?: return false
        val ws = day.weekStart()
        val wasPerfect = before.isPerfect(h, ws, today)
        val e = day.toEpochDay()
        Store.update(ctx) { it.setDone(h.id, e, false).setRating(h.id, e, null).setRest(h.id, e, true) }
        return !wasPerfect && Store.current(ctx).isPerfect(h, ws, today)
    }

    fun clear(ctx: Context, habitId: String, day: LocalDate) {
        val e = day.toEpochDay()
        Store.update(ctx) { it.setDone(habitId, e, false).setRest(habitId, e, false).setRating(habitId, e, null) }
    }
}
