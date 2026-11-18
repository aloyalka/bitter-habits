package com.zag.bitterhabits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zag.bitterhabits.data.Defaults
import com.zag.bitterhabits.data.Habit
import com.zag.bitterhabits.data.Store

@Composable
fun SetupScreen(onWriteOwn: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(Defaults.starters.indices.toSet()) }

    fun commit(markDone: Boolean) {
        Store.update(ctx) { d ->
            var n = d
            Defaults.starters.forEachIndexed { i, s ->
                if (i in picked && n.habits.none { it.name == s.name }) {
                    n = n.upsert(Habit(name = s.name, emblem = s.emblem, color = i % 5, freq = s.freq, weeklyTarget = s.target, restDays = s.rest))
                }
            }
            n.copy(setupDone = markDone || n.setupDone)
        }
    }

    if (step == 0) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(30.dp))
            Logo(30.dp)
            Spacer(Modifier.weight(1f))
            Text("Do the hard things.", style = ts(38.sp, FontWeight.SemiBold).copy(lineHeight = 42.sp))
            Text(
                "Small, uncomfortable, every day. That's the whole trick.",
                style = ts(17.sp, color = C.tx2).copy(lineHeight = 24.sp),
                modifier = Modifier.padding(top = 10.dp, bottom = 34.dp),
            )
            BigButton("Set up my habits", Modifier.fillMaxWidth()) { step = 1 }
            Spacer(Modifier.height(30.dp))
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo(24.dp)
            }
            Text("Your hard things.", style = ts(32.sp, FontWeight.SemiBold), modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 6.dp))
            Text(
                "Five to start. Untick any you don't want, or write your own.",
                style = ts(15.5.sp, color = C.tx2).copy(lineHeight = 21.sp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
            Defaults.starters.forEachIndexed { i, s ->
                val on = i in picked
                val color = habitColor(i)
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .tap { picked = if (on) picked - i else picked + i }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile { EmblemIcon(s.emblem, if (on) color else C.tx2, 20.dp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = ts(16.sp, FontWeight.Medium, if (on) C.tx else C.tx2))
                        Text(s.line, style = ts(12.5.sp, color = C.tx2))
                    }
                    SquareCheck(on, color)
                }
            }
            Text(
                "＋ Write your own",
                style = ts(15.sp, FontWeight.Medium),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .tap { commit(true); onWriteOwn() }
                    .padding(vertical = 8.dp),
            )
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(C.bg).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp)) {
            BigButton(if (picked.isEmpty()) "Start empty" else "Start with ${picked.size}", Modifier.fillMaxWidth()) {
                commit(true)
            }
        }
    }
}
