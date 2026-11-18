package com.zag.bitterhabits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zag.bitterhabits.MainActivity
import com.zag.bitterhabits.data.AppData
import com.zag.bitterhabits.data.DayState
import com.zag.bitterhabits.data.Habit
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.data.Ticks
import com.zag.bitterhabits.data.isPerfect
import com.zag.bitterhabits.data.state
import com.zag.bitterhabits.data.untickBreaksPerfect
import com.zag.bitterhabits.data.weekStart
import com.zag.bitterhabits.data.weekStates
import com.zag.bitterhabits.ui.Art
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val KEY_HABIT = ActionParameters.Key<String>(MainActivity.EXTRA_HABIT)
private val KEY_DAY = ActionParameters.Key<Long>(MainActivity.EXTRA_DAY)
private val LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

private val TX = Color(0xFFF2F5EE)
private val TX2 = Color(0xFF8E9A90)
private val TX3 = Color(0xFF56625A)

object Widgets {
    fun refresh(ctx: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try { HabitWidget().updateAll(ctx) } catch (e: Exception) { }
        }
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}

class HabitWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent {
            val data by Store.data.collectAsState()
            WidgetContent(context, data)
        }
    }
}

class ToggleDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val hid = parameters[KEY_HABIT] ?: return
        val day = parameters[KEY_DAY] ?: return
        Ticks.tap(context, hid, LocalDate.ofEpochDay(day))
        HabitWidget().updateAll(context)
    }
}

@Composable
private fun WidgetContent(ctx: Context, data: AppData) {
    val today = LocalDate.now()
    val ws = today.weekStart()
    val habits = data.active
    val due = habits.filter { data.state(it, today, today) != DayState.REST }
    val doneToday = due.count { data.state(it, today, today) == DayState.DONE }
    val f = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val we = ws.plusDays(6)
    val range = if (ws.month == we.month) "${ws.dayOfMonth}–${we.format(f)}" else "${ws.format(f)} – ${we.format(f)}"

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Color(0xF7111613))
            .cornerRadius(24.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(ImageProvider(WidgetArt.logo(ctx, 16f)), contentDescription = "Bitter", modifier = GlanceModifier.size(50.dp, 16.dp))
            Spacer(GlanceModifier.defaultWeight())
            Text("$range · ", style = TextStyle(color = ColorProvider(TX2), fontSize = 12.sp))
            Text("$doneToday of ${due.size} today", style = TextStyle(color = ColorProvider(TX), fontSize = 12.sp))
        }
        Row(GlanceModifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.5.dp))
            for (k in 0..6) {
                val isToday = ws.plusDays(k.toLong()) == today
                Box(GlanceModifier.width(17.dp), contentAlignment = Alignment.Center) {
                    Text(
                        LETTERS[k],
                        style = TextStyle(color = ColorProvider(if (isToday) TX else TX3), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(GlanceModifier.width(6.5.dp))
        }
        if (habits.isEmpty()) {
            Text(
                "No habits yet. Tap to add one.",
                style = TextStyle(color = ColorProvider(TX2), fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 12.dp).clickable(actionStartActivity<MainActivity>()),
            )
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(habits, itemId = { it.id.hashCode().toLong() }) { h ->
                    WidgetRow(ctx, data, h, ws, today)
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(ctx: Context, data: AppData, h: Habit, ws: LocalDate, today: LocalDate) {
    val color = Art.color(h.color).toInt()
    val states = data.weekStates(h, ws, today)
    val perfect = data.isPerfect(h, ws, today)
    Row(GlanceModifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(WidgetArt.emblem(ctx, h.emblem, color, 17f)), contentDescription = null, modifier = GlanceModifier.size(17.dp))
        Spacer(GlanceModifier.width(9.dp))
        Text(
            h.name,
            style = TextStyle(color = ColorProvider(TX), fontSize = 13.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Box(GlanceModifier.size(130.dp, 28.dp)) {
            Image(ImageProvider(WidgetArt.strip(ctx, states, perfect, color)), contentDescription = null, modifier = GlanceModifier.fillMaxSize())
            Row(GlanceModifier.fillMaxSize()) {
                Spacer(GlanceModifier.width(4.5.dp))
                for (k in 0..6) {
                    val d = ws.plusDays(k.toLong())
                    val params = actionParametersOf(KEY_HABIT to h.id, KEY_DAY to d.toEpochDay())
                    val action: Action? = when {
                        d.isAfter(today) -> null
                        data.untickBreaksPerfect(h, d, today) -> actionStartActivity<MainActivity>(params)
                        else -> actionRunCallback<ToggleDayAction>(params)
                    }
                    val m = GlanceModifier.width(17.dp).fillMaxHeight()
                    Box(if (action != null) m.clickable(action) else m) {}
                }
            }
        }
    }
}
