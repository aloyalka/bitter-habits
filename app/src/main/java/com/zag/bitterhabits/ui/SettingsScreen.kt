package com.zag.bitterhabits.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zag.bitterhabits.data.AppData
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.reminders.Reminders
import java.time.LocalDate

@Composable
fun SettingsScreen(data: AppData, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    BackHandler { onBack() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportJson(ctx).toByteArray()) }
                message = "Backup saved."
            } catch (e: Exception) { message = "Couldn't save the backup." }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                if (text != null) { Store.fromJson(text); pendingImport = text }
            } catch (e: Exception) { message = "That file isn't a Bitter Habits backup." }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).tap(onBack), contentAlignment = Alignment.Center) { Glyph("back") }
                Text("Settings", style = ts(20.sp, FontWeight.SemiBold))
            }

            Label("Habits", Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp))
            Text("Tap to edit. Use the arrows to reorder.", style = ts(13.sp, color = C.tx2), modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
            val active = data.active
            active.forEachIndexed { i, h ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp).height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).tap { onEdit(h.id) }, verticalAlignment = Alignment.CenterVertically) {
                        IconTile(size = 34) { EmblemIcon(h.emblem, habitColor(h.color), 18.dp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(h.name, style = ts(15.sp))
                            Text(goalLabel(h), style = ts(12.sp, color = C.tx2))
                        }
                    }
                    Box(Modifier.size(40.dp).tap { if (i > 0) Store.update(ctx) { it.move(h.id, -1) } }, contentAlignment = Alignment.Center) {
                        Glyph("up", if (i > 0) C.tx else C.chip, 20.dp)
                    }
                    Box(Modifier.size(40.dp).tap { if (i < active.size - 1) Store.update(ctx) { it.move(h.id, 1) } }, contentAlignment = Alignment.Center) {
                        Glyph("down", if (i < active.size - 1) C.tx else C.chip, 20.dp)
                    }
                }
            }

            Label("Archived", Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp))
            val archived = data.archivedHabits
            if (archived.isEmpty()) Text("Nothing archived.", style = ts(14.sp, color = C.tx2), modifier = Modifier.padding(start = 20.dp))
            archived.forEach { h ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(size = 34) { EmblemIcon(h.emblem, C.tx2, 18.dp) }
                    Spacer(Modifier.width(12.dp))
                    Text(h.name, style = ts(15.sp, color = C.tx2), modifier = Modifier.weight(1f))
                    Text("Restore", style = ts(15.sp, FontWeight.Medium, C.lime), modifier = Modifier.tap {
                        Store.update(ctx) { it.upsert(h.copy(archived = false)) }
                        Reminders.schedule(ctx, h.copy(archived = false))
                    })
                }
            }

            Label("Backup", Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionRow("Export backup", "Saves a file with all your habits and history.") {
                    exporter.launch("bitter-habits-${LocalDate.now()}.json")
                }
                ActionRow("Import backup", "Replaces everything on this phone with a backup file.") {
                    importer.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }

            Label("About", Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp))
            Text("Bitter Habits", style = ts(15.sp), modifier = Modifier.padding(start = 20.dp))
            Text(
                "Made by Bitter. Be comfortable with the uncomfortable.",
                style = ts(13.sp, color = C.tx2), modifier = Modifier.padding(start = 20.dp, top = 2.dp),
            )
        }

        message?.let {
            Sheet({ message = null }) {
                Text(it, style = ts(20.sp, FontWeight.Medium), modifier = Modifier.padding(bottom = 20.dp))
                BigButton("OK", Modifier.fillMaxWidth()) { message = null }
            }
        }
        pendingImport?.let { text ->
            Sheet({ pendingImport = null }) {
                Text("Replace everything?", style = ts(26.sp, FontWeight.SemiBold))
                Text(
                    "This replaces every habit and day on this phone with the backup.",
                    style = ts(15.sp, color = C.tx2), modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Cancel", Modifier.weight(1f), ghost = true) { pendingImport = null }
                    BigButton("Replace", Modifier.weight(1f)) {
                        Store.replaceAll(ctx, text)
                        Reminders.scheduleAll(ctx)
                        pendingImport = null
                        message = "Backup restored."
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(title: String, sub: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.surf, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .tap(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(title, style = ts(15.5.sp))
        Text(sub, style = ts(12.5.sp, color = C.tx2))
    }
}
