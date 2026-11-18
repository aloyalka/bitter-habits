package com.zag.bitterhabits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Clickable without the ripple, which looks out of place on this flat UI. */
fun Modifier.tap(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
)

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.surf)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (i == selected) C.chip else Color.Transparent)
                    .tap { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = ts(14.5.sp, FontWeight.Medium, if (i == selected) C.tx else C.tx2))
            }
        }
    }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(46.dp, 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) C.lime else C.toggleOff)
            .tap { onChange(!on) }
    ) {
        Box(
            Modifier
                .offset(x = if (on) 21.dp else 3.dp, y = 3.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (on) C.bg else C.tx2)
        )
    }
}

@Composable
fun SettingRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.surf)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.5.sp))
            Text(sub, style = ts(12.5.sp, color = C.tx2).copy(lineHeight = 17.sp))
        }
        Spacer(Modifier.width(12.dp))
        Toggle(on, onChange)
    }
}

@Composable
fun BigButton(label: String, modifier: Modifier = Modifier, ghost: Boolean = false, color: Color = C.lime, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (ghost) C.surf else color)
            .tap(onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = ts(16.5.sp, FontWeight.SemiBold, if (ghost) C.tx else C.bg))
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = ts(11.sp, FontWeight.Medium, C.tx2, 1.4.sp), modifier = modifier)
}

@Composable
fun IconTile(modifier: Modifier = Modifier, size: Int = 38, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(C.surf),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(C.surf)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Label(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = ts(26.sp, FontWeight.Medium))
            Spacer(Modifier.width(5.dp))
            Text(unit, style = ts(13.sp, color = C.tx2), modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

/** Bottom sheet over a dimmed screen. */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x9E000000))
                .tap(onDismiss)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(C.surf)
                .tap { }
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 30.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(40.dp, 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E3A31))
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
fun SheetOption(text: String, color: Color = C.tx, onClick: () -> Unit) {
    Text(
        text,
        style = ts(17.sp, FontWeight.Medium, color),
        modifier = Modifier
            .fillMaxWidth()
            .tap(onClick)
            .padding(vertical = 13.dp),
    )
}

@Composable
fun Banner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.surf2)
            .border(1.dp, C.chip, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Text(text, style = ts(15.sp, FontWeight.Medium), textAlign = TextAlign.Center)
    }
}

@Composable
fun SquareCheck(on: Boolean, color: Color, size: Int = 24) {
    androidx.compose.foundation.Canvas(Modifier.size(size.dp)) {
        val s = this.size.width
        drawCell(
            if (on) com.zag.bitterhabits.data.DayState.DONE else com.zag.bitterhabits.data.DayState.OPEN,
            0f, 0f, s, s * 0.27f, color, 1.5.dp.toPx(), false,
        )
    }
}

@Composable
fun rememberPrinciple(dayOfYear: Int): String = remember(dayOfYear) { Art.principles[dayOfYear % Art.principles.size] }

val rowGap = Arrangement.spacedBy(10.dp)
