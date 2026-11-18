package com.zag.bitterhabits.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zag.bitterhabits.R

object C {
    val bg = Color(0xFF0B0F0D)
    val surf = Color(0xFF141A16)
    val surf2 = Color(0xFF1B231E)
    val chip = Color(0xFF232C26)
    val line = Color(0xFF1A221D)
    val tx = Color(0xFFF2F5EE)
    val tx2 = Color(0xFF8E9A90)
    val tx3 = Color(0xFF56625A)
    val lime = Color(0xFFE0F72E)
    val open = Color(0xFF3E4842)
    val future = Color(0xFF232B26)
    val toggleOff = Color(0xFF2A342D)
}

val Jost = FontFamily(
    Font(R.font.jost_regular, FontWeight.Normal),
    Font(R.font.jost_medium, FontWeight.Medium),
    Font(R.font.jost_semibold, FontWeight.SemiBold),
)

fun habitColor(index: Int): Color = Color(Art.color(index))

fun ts(size: TextUnit = 15.sp, weight: FontWeight = FontWeight.Normal, color: Color = C.tx, spacing: TextUnit = 0.sp) =
    TextStyle(fontFamily = Jost, fontSize = size, fontWeight = weight, color = color, letterSpacing = spacing)

@Composable
fun BitterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.lime, onPrimary = C.bg, background = C.bg, surface = C.surf,
            onSurface = C.tx, onBackground = C.tx,
        ),
        content = content,
    )
}
