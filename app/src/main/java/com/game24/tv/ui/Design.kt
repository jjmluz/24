package com.game24.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.game24.tv.game.Difficulty
import com.game24.tv.ui.theme.Game24Colors as C
import kotlin.math.min

// Screens are laid out on the 1280×720 grid of the canvas mockups and scaled to the TV.
internal const val DESIGN_W = 1280f
internal const val DESIGN_H = 720f

/** Converts design-grid units to dp, and to sp that ignore the system font scale so text always fits. */
@Stable
internal class Design(private val unit: Float, private val fontScale: Float) {
    fun dp(v: Number): Dp = (v.toFloat() * unit).dp
    fun sp(v: Number): TextUnit = (v.toFloat() * unit / fontScale).sp
}

/** Full-screen surface with a centred, scaled 1280×720 design area. */
@Composable
internal fun DesignCanvas(modifier: Modifier = Modifier, content: @Composable BoxScope.(Design) -> Unit) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(C.Surface),
        contentAlignment = Alignment.Center,
    ) {
        val d = Design(
            unit = min(maxWidth.value / DESIGN_W, maxHeight.value / DESIGN_H),
            fontScale = LocalDensity.current.fontScale,
        )
        Box(Modifier.size(d.dp(DESIGN_W), d.dp(DESIGN_H))) { content(d) }
    }
}

/**
 * Material 3 pill button in the Google TV style: focused buttons turn light and grow slightly.
 * [tonal] gives an unfocused secondary-container fill; otherwise it is a text button.
 */
@Composable
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    d: Design,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    height: Int = 56,
    fontSize: Int = 18,
    leading: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(d.dp(height)),
        shape = ButtonDefaults.shape(RoundedCornerShape(d.dp(height / 2))),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        colors = ButtonDefaults.colors(
            containerColor = if (tonal) C.SecondaryContainer else Color.Transparent,
            contentColor = if (tonal) C.OnSecondaryContainer else C.OnSurface,
            focusedContainerColor = C.InverseSurface,
            focusedContentColor = C.InverseOnSurface,
            pressedContainerColor = C.InverseSurface,
            pressedContentColor = C.InverseOnSurface,
        ),
        contentPadding = PaddingValues(horizontal = d.dp(28)),
    ) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(
                text,
                fontWeight = FontWeight.Medium,
                style = pillLabelStyle(d, fontSize),
            )
        }
    }
}

/** Trims the font's extra top/bottom padding so the label sits in the optical centre. */
internal fun pillLabelStyle(d: Design, fontSize: Int) = TextStyle(
    fontSize = d.sp(fontSize),
    lineHeight = d.sp(fontSize),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** One white dot, two red or three yellow, like the marks on the official cards. */
internal val Difficulty.dotColors: List<Color>
    get() = when (this) {
        Difficulty.EASY -> listOf(C.DotEasy)
        Difficulty.MEDIUM -> List(2) { C.DotMedium }
        Difficulty.HARD -> List(3) { C.DotHard }
    }

@Composable
internal fun Dots(colors: List<Color>, size: Dp, gap: Dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        colors.forEach { Box(Modifier.size(size).background(it, CircleShape)) }
    }
}

/** Keeps D-pad focus inside a dialog so it can't wander onto the board behind it. */
internal fun Modifier.trapFocus(): Modifier = focusProperties { onExit = { cancelFocusChange() } }.focusGroup()
