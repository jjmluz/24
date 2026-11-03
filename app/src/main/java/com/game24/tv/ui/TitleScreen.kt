package com.game24.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.game24.tv.R
import com.game24.tv.game.Difficulty
import com.game24.tv.game.DifficultyChoice
import com.game24.tv.ui.theme.Game24Colors as C

@Composable
fun TitleScreen(
    choice: DifficultyChoice,
    onChoose: (DifficultyChoice) -> Unit,
    onPlay: () -> Unit,
) {
    val play = remember { FocusRequester() }
    LaunchedEffect(Unit) { play.requestFocus() }

    DesignCanvas { d ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.dp(28), Alignment.CenterVertically),
        ) {
            Text(
                stringResource(R.string.title),
                color = C.Primary,
                fontSize = d.sp(220),
                fontWeight = FontWeight.Bold,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.dp(8))) {
                Text(stringResource(R.string.title_tagline), color = C.OnSurface, fontSize = d.sp(36), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.title_rules), color = C.OnSurfaceVariant, fontSize = d.sp(18))
            }
            DifficultyPicker(choice, onChoose, d)
            PillButton(
                text = stringResource(R.string.play),
                onClick = onPlay,
                d = d,
                height = 64,
                fontSize = 22,
                modifier = Modifier.focusRequester(play),
                leading = { PlayIcon(Modifier.padding(end = d.dp(12)).size(d.dp(24))) },
            )
        }
    }
}

@Composable
private fun DifficultyPicker(choice: DifficultyChoice, onChoose: (DifficultyChoice) -> Unit, d: Design) {
    val selected = remember { FocusRequester() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.dp(16)),
    ) {
        Text(stringResource(R.string.difficulty), color = C.OnSurfaceVariant, fontSize = d.sp(18))
        // Coming up from Jogar lands on the selected option, not the nearest one.
        Row(
            modifier = Modifier.focusRestorer(selected).focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(d.dp(12)),
        ) {
            DifficultyChoice.entries.forEach { option ->
                DifficultyChip(
                    option = option,
                    selected = option == choice,
                    onClick = { onChoose(option) },
                    d = d,
                    modifier = if (option == choice) Modifier.focusRequester(selected) else Modifier,
                )
            }
        }
    }
}

/**
 * The fill never changes, because white and yellow dots would vanish on a white or yellow chip.
 * A yellow ring and label mark the selected option; a white ring marks focus.
 */
@Composable
private fun DifficultyChip(
    option: DifficultyChoice,
    selected: Boolean,
    onClick: () -> Unit,
    d: Design,
    modifier: Modifier = Modifier,
) {
    val content = if (selected) C.Primary else C.OnSurface
    Button(
        onClick = onClick,
        modifier = modifier.height(d.dp(72)).semantics { this.selected = selected },
        shape = ButtonDefaults.shape(RoundedCornerShape(d.dp(36))),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        colors = ButtonDefaults.colors(
            containerColor = C.SurfaceContainerHigh,
            contentColor = content,
            focusedContainerColor = C.SurfaceContainerHigh,
            focusedContentColor = content,
            pressedContainerColor = C.SurfaceContainerHigh,
            pressedContentColor = content,
        ),
        border = ButtonDefaults.border(
            border = if (selected) Border(BorderStroke(3.dp, C.Primary)) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, C.InverseSurface)),
        ),
        contentPadding = PaddingValues(horizontal = d.dp(28)),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.dp(8), Alignment.CenterVertically),
        ) {
            Text(stringResource(option.label), fontWeight = FontWeight.Medium, style = pillLabelStyle(d, 18))
            Dots(option.dotColors, size = d.dp(10), gap = d.dp(5))
        }
    }
}

/** Todas shows one dot of each level. */
private val DifficultyChoice.dotColors
    get() = difficulty?.dotColors ?: Difficulty.entries.map { it.dotColors.first() }

private val DifficultyChoice.label
    get() = when (this) {
        DifficultyChoice.ALL -> R.string.difficulty_all
        DifficultyChoice.EASY -> R.string.difficulty_easy
        DifficultyChoice.MEDIUM -> R.string.difficulty_medium
        DifficultyChoice.HARD -> R.string.difficulty_hard
    }

@Composable
private fun PlayIcon(modifier: Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.2f)
            lineTo(size.width * 0.3f, size.height * 0.8f)
            lineTo(size.width * 0.8f, size.height * 0.5f)
            close()
        }
        drawPath(path, color)
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TitleScreenPreview() {
    TitleScreen(choice = DifficultyChoice.ALL, onChoose = {}, onPlay = {})
}
