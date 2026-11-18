package com.zag.bitterhabits.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zag.bitterhabits.data.AppData
import com.zag.bitterhabits.data.DayState
import com.zag.bitterhabits.data.Freq
import com.zag.bitterhabits.data.Habit
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.reminders.Reminders
import java.util.Locale

@Composable
fun EditScreen(data: AppData, habitId: String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val existing = habitId?.let { data.habit(it) }
    val base = existing ?: Habit(name = "", emblem = "squig", color = data.habits.size % 5)

    var name by remember { mutableStateOf(base.name) }
    var emblem by remember { mutableStateOf(base.emblem) }
    var color by remember { mutableIntStateOf(base.color) }
    var freq by remember { mutableStateOf(base.freq) }
    var target by remember { mutableIntStateOf(base.weeklyTarget) }
    var rest by remember { mutableStateOf(base.restDays) }
    var reminderOn by remember { mutableStateOf(base.reminderOn) }
    var minutes by remember { mutableIntStateOf(base.reminderMinutes) }
    var rateOn by remember { mutableStateOf(base.rateOn) }
    var showAllEmblems by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminderOn = granted
    }

    BackHandler { onDone() }

    fun save() {
        if (name.isBlank()) { nameError = true; return }
        val h = base.copy(
            name = name.trim(), emblem = emblem, color = color, freq = freq, weeklyTarget = target,
            restDays = if (freq == Freq.DAILY) rest else emptySet(),
            reminderOn = reminderOn, reminderMinutes = minutes, rateOn = rateOn,
        )
        Store.update(ctx) { it.upsert(h).copy(setupDone = true) }
        Reminders.schedule(ctx, h)
        onDone()
    }

    val accent = habitColor(color)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", style = ts(16.sp, color = C.tx2), modifier = Modifier.tap(onDone))
                Text(
                    if (existing == null) "New habit" else "Edit habit",
                    style = ts(17.sp, FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text("Save", style = ts(16.sp, FontWeight.SemiBold, C.lime), modifier = Modifier.tap { save() })
            }

            Label("What's the hard thing?", Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp))
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.surf)
                    .border(1.dp, if (nameError) Color(0xFFFF7A1F) else Color.Transparent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                if (name.isEmpty()) Text("e.g. Cold shower", style = ts(17.sp, color = C.tx3))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(40); nameError = false },
                    textStyle = ts(17.sp),
                    cursorBrush = SolidColor(C.lime),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (nameError) Text("Give it a name.", style = ts(13.sp, color = Color(0xFFFF7A1F)), modifier = Modifier.padding(start = 20.dp, top = 6.dp))

            Label("Emblem", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
            val list = if (showAllEmblems) Art.emblems else Art.emblems.take(12).let { if (it.any { e -> e.key == emblem }) it else it.dropLast(1) + Art.emblem(emblem) }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.chunked(6).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { e ->
                            val on = e.key == emblem
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(C.surf)
                                    .border(1.5.dp, if (on) accent else Color.Transparent, RoundedCornerShape(11.dp))
                                    .tap { emblem = e.key },
                                contentAlignment = Alignment.Center,
                            ) { EmblemIcon(e.key, if (on) accent else Color(0xFFC9D1C6), 21.dp) }
                        }
                        repeat(6 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text(
                if (showAllEmblems) "Show fewer" else "See all ${Art.emblems.size}",
                style = ts(13.sp, color = C.tx2),
                modifier = Modifier.padding(start = 20.dp, top = 10.dp).tap { showAllEmblems = !showAllEmblems },
            )

            Label("Colour", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp))
            Row(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Art.habitColors.indices.forEach { i ->
                    val on = i == color
                    Box(
                        Modifier
                            .size(48.dp)
                            .border(2.dp, if (on) habitColor(i) else Color.Transparent, RoundedCornerShape(15.dp))
                            .padding(4.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(habitColor(i))
                            .tap { color = i }
                    )
                }
            }

            Label("How often", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp))
            Segmented(
                listOf("Every day", "Times a week"),
                if (freq == Freq.DAILY) 0 else 1,
                { freq = if (it == 0) Freq.DAILY else Freq.WEEKLY },
                Modifier.padding(horizontal = 20.dp),
            )
            if (freq == Freq.WEEKLY) {
                Row(
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.surf)
                        .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$target", style = ts(22.sp, FontWeight.SemiBold, accent))
                    Text("  times a week", style = ts(16.sp), modifier = Modifier.weight(1f))
                    StepButton("−") { if (target > 1) target-- }
                    Spacer(Modifier.width(8.dp))
                    StepButton("+") { if (target < 6) target++ }
                }
                Text(
                    "Hit $target and the rest of the week counts as rest. Anything extra is a bonus.",
                    style = ts(13.5.sp, color = C.tx2).copy(lineHeight = 19.sp),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                )
            } else {
                Row(Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp)) {
                    Text("Rest days", style = ts(13.5.sp))
                    Text(" · tap the days you're off", style = ts(13.5.sp, color = C.tx2))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, l ->
                        val day = i + 1
                        val on = day in rest
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.tap {
                            rest = if (on) rest - day else rest + day
                        }) {
                            androidx.compose.foundation.Canvas(Modifier.size(34.dp)) {
                                drawCell(if (on) DayState.REST else DayState.OPEN, 1f, 1f, size.width - 2f, 9.dp.toPx(), accent, 1.5.dp.toPx(), false)
                            }
                            Text(l, style = ts(12.sp, color = C.tx2), modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
                Text(
                    "Planned days off. They show as rest this week and never break a streak.",
                    style = ts(12.5.sp, color = C.tx2).copy(lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            val timeText = String.format(Locale.ENGLISH, "%d:%02d %s", ((minutes / 60) + 11) % 12 + 1, minutes % 60, if (minutes < 720) "am" else "pm")
            SettingRow(
                "Reminder",
                if (reminderOn) "Every day at $timeText. Tap here to change the time." else "Off. Turn it on if you want a nudge.",
                reminderOn,
                { on ->
                    if (!on) reminderOn = false
                    else if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else reminderOn = true
                },
                Modifier.padding(horizontal = 20.dp).tap {
                    if (reminderOn) TimePickerDialog(ctx, { _, hh, mm -> minutes = hh * 60 + mm }, minutes / 60, minutes % 60, false).show()
                },
            )
            Spacer(Modifier.height(8.dp))
            SettingRow(
                "Rate how bitter it was",
                if (rateOn) "On. You'll be asked 1 to 5 each time you tick it, with a weekly average and a trend."
                else "Off. Turn on to rate this habit 1 to 5 each time you tick it. You'll get a weekly average and can see if it's getting easier.",
                rateOn, { rateOn = it },
                Modifier.padding(horizontal = 20.dp),
            )

            if (existing != null) {
                Spacer(Modifier.height(26.dp))
                Text(
                    if (existing.archived) "Restore habit" else "Archive habit",
                    style = ts(16.sp, FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 20.dp).tap {
                        Store.update(ctx) { it.upsert(existing.copy(archived = !existing.archived)) }
                        Reminders.schedule(ctx, existing.copy(archived = !existing.archived))
                        onDone()
                    },
                )
                Text(
                    "Archived habits keep their history. Bring them back any time.",
                    style = ts(12.5.sp, color = C.tx2), modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                )
                Text(
                    "Delete habit",
                    style = ts(16.sp, FontWeight.Medium, Color(0xFFFF7A1F)),
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp).tap { confirmDelete = true },
                )
            }
        }

        if (confirmDelete && existing != null) {
            Sheet({ confirmDelete = false }) {
                Text("Delete ${existing.name}?", style = ts(26.sp, FontWeight.SemiBold))
                Text(
                    "This deletes every day you've logged for it. It can't be undone.",
                    style = ts(15.sp, color = C.tx2).copy(lineHeight = 21.sp),
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Cancel", Modifier.weight(1f), ghost = true) { confirmDelete = false }
                    BigButton("Delete", Modifier.weight(1f), color = Color(0xFFFF7A1F)) {
                        Reminders.cancel(ctx, existing.id)
                        Store.update(ctx) { it.delete(existing.id) }
                        onDone()
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.chip)
            .tap(onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = ts(22.sp)) }
}
