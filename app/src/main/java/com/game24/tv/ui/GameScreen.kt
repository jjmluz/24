package com.game24.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import com.game24.tv.R
import com.game24.tv.game.Card
import com.game24.tv.game.Difficulty
import com.game24.tv.game.GameState
import com.game24.tv.game.Op
import com.game24.tv.game.Phase
import com.game24.tv.game.Slot
import com.game24.tv.game.TARGET
import com.game24.tv.game.starsFor
import com.game24.tv.ui.theme.Game24Colors as C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val cardCenters = mapOf(
    Slot.UP to Offset(640f, 100f),
    Slot.LEFT to Offset(340f, 300f),
    Slot.RIGHT to Offset(940f, 300f),
    Slot.DOWN to Offset(640f, 500f),
)
private val opCenters = mapOf(
    Slot.UP to Offset(640f, 216f),
    Slot.LEFT to Offset(520f, 300f),
    Slot.RIGHT to Offset(760f, 300f),
    Slot.DOWN to Offset(640f, 384f),
)
private const val CARD_W = 112f
private const val CARD_H = 136f
private const val OP_SIZE = 68f

/** Remembers whether the current Back press already opened the pause menu by being held. */
private class BackPress {
    var heldOpenedPause = false
}

@Composable
fun GameScreen(vm: GameViewModel, onQuit: () -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val puzzle by vm.puzzle.collectAsStateWithLifecycle()
    val difficulty by vm.difficulty.collectAsStateWithLifecycle()
    val paused by vm.paused.collectAsStateWithLifecycle()

    // Leaving the app (Home, switching inputs) opens the pause menu and stops the clock.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.pause() }

    val dialogOpen = paused || game.phase == Phase.SOLVED
    val focus = remember { FocusRequester() }
    LaunchedEffect(dialogOpen) { if (!dialogOpen) focus.requestFocus() }
    val back = remember { BackPress() }

    DesignCanvas(
        modifier = Modifier
            .onKeyEvent { e ->
                // Back is handled here rather than with BackHandler so a long press can be told apart.
                // Short press: undo (or pause when there's nothing to undo). Hold: pause.
                if (e.key == Key.Back) {
                    when (e.type) {
                        KeyEventType.KeyDown ->
                            if (!dialogOpen && e.nativeKeyEvent.isLongPress) {
                                back.heldOpenedPause = true
                                vm.pause()
                            }
                        KeyEventType.KeyUp -> when {
                            back.heldOpenedPause -> back.heldOpenedPause = false
                            paused -> vm.resume()
                            game.phase == Phase.SOLVED -> onQuit()
                            game.canUndo -> vm.onBack()
                            game.phase == Phase.PICK_FIRST -> vm.pause()
                        }
                    }
                    return@onKeyEvent true
                }
                // While a dialog is open, its buttons own the D-pad.
                if (dialogOpen || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                // Ignore auto-repeat so holding a direction can't also pick the next operator.
                if (e.nativeKeyEvent.repeatCount > 0) return@onKeyEvent true
                when (e.key) {
                    Key.DirectionUp -> vm.onDirection(Slot.UP)
                    Key.DirectionDown -> vm.onDirection(Slot.DOWN)
                    Key.DirectionLeft -> vm.onDirection(Slot.LEFT)
                    Key.DirectionRight -> vm.onDirection(Slot.RIGHT)
                    else -> return@onKeyEvent false
                }
                true
            }
            .focusRequester(focus)
            .focusable(),
    ) { d ->
        GameBoard(game, vm.seconds, puzzle, difficulty, d)
        when {
            paused -> PauseDialog(
                seconds = vm.seconds.value,
                d = d,
                onResume = vm::resume,
                onRestart = vm::restartPuzzle,
                onNewPuzzle = vm::newPuzzle,
                onQuit = onQuit,
            )
            game.phase == Phase.SOLVED -> SolvedDialog(
                game = game,
                seconds = vm.seconds.value,
                stars = starsFor(vm.seconds.value),
                puzzle = puzzle,
                d = d,
                onNext = vm::nextPuzzle,
                onQuit = onQuit,
            )
        }
    }
}

@Composable
private fun GameBoard(game: GameState, seconds: StateFlow<Int>, puzzle: Int, difficulty: Difficulty, d: Design) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.offset(d.dp(48), d.dp(40))) {
            Text(stringResource(R.string.puzzle_label), color = C.OnSurfaceVariant, fontSize = d.sp(14), fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.dp(12)),
            ) {
                Text("$puzzle", color = C.OnSurface, fontSize = d.sp(32), fontWeight = FontWeight.Medium)
                DifficultyDots(difficulty, d)
            }
        }
        TimerChip(
            seconds = seconds,
            d = d,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = d.dp(44), end = d.dp(48)),
        )

        val cardsActive = game.phase != Phase.PICK_OP
        Slot.entries.forEach { slot ->
            val c = cardCenters.getValue(slot)
            NumberCard(
                card = game.visibleCard(slot),
                active = cardsActive,
                d = d,
                modifier = Modifier
                    .offset(d.dp(c.x - CARD_W / 2), d.dp(c.y - CARD_H / 2))
                    .size(d.dp(CARD_W), d.dp(CARD_H)),
            )
        }
        Slot.entries.forEach { slot ->
            val c = opCenters.getValue(slot)
            val op = Op.forSlot(slot)
            OpBubble(
                op = op,
                selected = game.op == op && (game.phase == Phase.PICK_SECOND || game.phase == Phase.MERGING),
                active = game.phase == Phase.PICK_OP,
                d = d,
                modifier = Modifier
                    .offset(d.dp(c.x - OP_SIZE / 2), d.dp(c.y - OP_SIZE / 2))
                    .size(d.dp(OP_SIZE)),
            )
        }

        EquationRow(
            game = game,
            d = d,
            modifier = Modifier.offset(d.dp(320), d.dp(596)).size(d.dp(640), d.dp(84)),
        )

        val keyHint = when {
            game.phase == Phase.SOLVED -> R.string.key_ok to R.string.next_puzzle
            game.phase == Phase.MERGING || game.phase == Phase.MISS -> null
            game.canUndo -> R.string.key_back to R.string.action_undo
            else -> R.string.key_back to R.string.action_pause
        }
        val keyHintSub = if (keyHint?.second == R.string.action_undo) stringResource(R.string.action_undo_sub) else null
        if (keyHint != null) Row(
            modifier = Modifier.offset(d.dp(48), d.dp(596)).size(d.dp(260), d.dp(84)),
            horizontalArrangement = Arrangement.spacedBy(d.dp(10)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(keyHint.first),
                color = C.OnSurfaceVariant,
                fontSize = d.sp(13),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .border(1.dp, C.Outline, RoundedCornerShape(d.dp(8)))
                    .padding(horizontal = d.dp(10), vertical = d.dp(5)),
            )
            Column {
                Text(stringResource(keyHint.second), color = C.OnSurface, fontSize = d.sp(16), fontWeight = FontWeight.Medium)
                if (keyHintSub != null) Text(keyHintSub, color = C.OnSurfaceVariant, fontSize = d.sp(14))
            }
        }

        val (hintTitleRes, hintSubRes) = hintFor(game)
        val hintTitle = stringResource(hintTitleRes, TARGET)
        val hintSub = hintSubRes?.let { stringResource(it) }.orEmpty()
        Row(
            modifier = Modifier.offset(d.dp(972), d.dp(596)).size(d.dp(260), d.dp(84)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.dp(14), Alignment.End),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(hintTitle, color = C.OnSurface, fontSize = d.sp(18), fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                if (hintSub.isNotEmpty()) Text(hintSub, color = C.OnSurfaceVariant, fontSize = d.sp(14), textAlign = TextAlign.End)
            }
            DpadIcon(Modifier.size(d.dp(44)))
        }

    }
}

/** Title and subtitle string resources for the hint in the bottom-right corner. */
private fun hintFor(game: GameState): Pair<Int, Int?> = when (game.phase) {
    Phase.PICK_FIRST -> R.string.hint_pick_first to null
    Phase.PICK_OP -> R.string.hint_pick_op to null
    Phase.PICK_SECOND -> R.string.hint_pick_second to
        if (game.divideByZero) R.string.hint_divide_by_zero else R.string.hint_pick_second_sub
    Phase.MERGING -> R.string.hint_merging to null
    Phase.MISS -> R.string.hint_miss to R.string.hint_miss_sub
    Phase.SOLVED -> R.string.hint_solved to R.string.hint_solved_sub
}

@Composable
private fun DifficultyDots(difficulty: Difficulty, d: Design) {
    val description = stringResource(
        when (difficulty) {
            Difficulty.EASY -> R.string.difficulty_easy
            Difficulty.MEDIUM -> R.string.difficulty_medium
            Difficulty.HARD -> R.string.difficulty_hard
        },
    )
    Dots(
        colors = difficulty.dotColors,
        size = d.dp(14),
        gap = d.dp(6),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/** Fractions can reach nine characters (−1716/143), which must still fit on a card. */
private fun cardFontSize(label: String) = when (label.length) {
    in 0..2 -> 64
    in 3..4 -> 40
    5 -> 32
    in 6..7 -> 24
    else -> 19
}

@Composable
private fun NumberCard(card: Card?, active: Boolean, d: Design, modifier: Modifier) {
    val (bg, fg) = when {
        card == null -> Color.Transparent to Color.Transparent
        !active -> C.OnSurface.copy(alpha = 0.12f) to C.OnSurface.copy(alpha = 0.38f)
        card.isResult -> C.TertiaryContainer to C.OnTertiaryContainer
        else -> C.PrimaryContainer to C.OnPrimaryContainer
    }
    // Colours are read while drawing, so a fade doesn't recompose the card on every frame.
    val bgAnim = animateColorAsState(bg, label = "cardBg")
    val fgAnim = animateColorAsState(fg, label = "cardFg")

    // Pop in whenever a card lands in this slot (new result, undo, or reset after a miss).
    val pop = remember(card) { Animatable(if (card == null) 1f else 0.7f) }
    LaunchedEffect(card) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .drawBehind { drawRoundRect(bgAnim.value, cornerRadius = CornerRadius(d.dp(28).toPx())) }
            .then(if (card == null) Modifier.dashedBorder(d.dp(2), C.OutlineVariant, d.dp(28)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (card != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = card.value.toString()
                FadingText(label, { fgAnim.value }, d.sp(cardFontSize(label)), FontWeight.SemiBold)
                if (card.isResult) {
                    FadingText(
                        card.expr,
                        { fgAnim.value.let { it.copy(alpha = it.alpha * 0.8f) } },
                        d.sp(14),
                        FontWeight.Medium,
                        Modifier.padding(horizontal = d.dp(8)),
                    )
                }
            }
        }
    }
}

/** One line of text whose colour is read while drawing. */
@Composable
private fun FadingText(
    text: String,
    color: ColorProducer,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text,
        modifier,
        style = LocalTextStyle.current.merge(TextStyle(fontSize = fontSize, fontWeight = fontWeight)),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        color = color,
    )
}

@Composable
private fun OpBubble(op: Op, selected: Boolean, active: Boolean, d: Design, modifier: Modifier) {
    val bg = when {
        selected -> C.Primary
        active -> C.SecondaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        selected -> C.OnPrimary
        active -> C.OnSecondaryContainer
        else -> C.OnSurface.copy(alpha = 0.38f)
    }
    val bgAnim = animateColorAsState(bg, label = "opBg")
    val fgAnim = animateColorAsState(fg, label = "opFg")
    Box(
        modifier = modifier
            .drawBehind { drawCircle(bgAnim.value) }
            .then(if (!selected && !active) Modifier.border(1.dp, C.OutlineVariant, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        FadingText(op.symbol, { fgAnim.value }, d.sp(36), FontWeight.Medium)
    }
}

private enum class TokenKind { NUM, OP, EQ, NOTE, RESULT, WIN, MISS }

private data class Token(val text: String, val kind: TokenKind)

private fun tokensFor(game: GameState, missNote: String): List<Token> {
    val m = game.merge
    return when (game.phase) {
        Phase.PICK_FIRST -> emptyList()
        Phase.PICK_OP -> listOfNotNull(game.firstCard?.let { Token(it.value.toString(), TokenKind.NUM) })
        Phase.PICK_SECOND -> listOfNotNull(
            game.firstCard?.let { Token(it.value.toString(), TokenKind.NUM) },
            game.op?.let { Token(it.symbol, TokenKind.OP) },
        )
        Phase.MERGING, Phase.SOLVED -> if (m == null) emptyList() else listOf(
            Token(m.first.value.toString(), TokenKind.NUM),
            Token(m.op.symbol, TokenKind.OP),
            Token(m.second.value.toString(), TokenKind.NUM),
            Token("=", TokenKind.EQ),
            Token(m.result.value.toString(), if (game.phase == Phase.SOLVED) TokenKind.WIN else TokenKind.RESULT),
        )
        Phase.MISS -> if (m == null) emptyList() else listOf(
            Token(m.result.value.toString(), TokenKind.MISS),
            Token(missNote, TokenKind.NOTE),
        )
    }
}

@Composable
private fun EquationRow(game: GameState, d: Design, modifier: Modifier) {
    val shape = RoundedCornerShape(d.dp(42))
    val tokens = tokensFor(game, stringResource(R.string.miss_note, TARGET))
    Row(
        modifier = modifier
            .background(C.SurfaceContainerHigh, shape)
            .then(if (game.phase == Phase.MERGING) Modifier.border(2.dp, C.Primary, shape) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(d.dp(18), Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tokens.isEmpty()) {
            val text = if (game.history.isEmpty()) R.string.placeholder_start else R.string.placeholder_next
            Text(stringResource(text), color = C.OnSurfaceVariant, fontSize = d.sp(20))
        }
        tokens.forEach { t -> EquationToken(t, d) }
    }
}

@Composable
private fun EquationToken(t: Token, d: Design) {
    val chip = RoundedCornerShape(d.dp(16))
    val chipPadding = Modifier.padding(horizontal = d.dp(20), vertical = d.dp(6))
    when (t.kind) {
        TokenKind.NUM -> Text(t.text, color = C.OnSurface, fontSize = d.sp(44), fontWeight = FontWeight.Medium)
        TokenKind.OP -> Text(t.text, color = C.Primary, fontSize = d.sp(40), fontWeight = FontWeight.Medium)
        TokenKind.EQ -> Text(t.text, color = C.OnSurfaceVariant, fontSize = d.sp(40))
        TokenKind.NOTE -> Text(t.text, color = C.OnSurfaceVariant, fontSize = d.sp(26), fontWeight = FontWeight.Medium)
        TokenKind.RESULT, TokenKind.WIN, TokenKind.MISS -> {
            val (bg, fg) = when (t.kind) {
                TokenKind.WIN -> C.Primary to C.OnPrimary
                TokenKind.MISS -> C.ErrorContainer to C.OnErrorContainer
                else -> C.TertiaryContainer to C.OnTertiaryContainer
            }
            Text(
                t.text,
                color = fg,
                fontSize = d.sp(44),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(bg, chip).then(chipPadding),
            )
        }
    }
}

@Composable
private fun TimerChip(seconds: StateFlow<Int>, d: Design, modifier: Modifier) {
    // Collected here so the clock's tick redraws this chip rather than the whole board.
    val time by seconds.collectAsStateWithLifecycle()
    Row(
        modifier = modifier
            .height(d.dp(48))
            .background(C.SurfaceContainerHigh, RoundedCornerShape(d.dp(24)))
            .padding(start = d.dp(16), end = d.dp(20)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.dp(12)),
    ) {
        TimerIcon(Modifier.size(d.dp(22)))
        Text(formatTime(time), color = C.OnSurface, fontSize = d.sp(22), fontWeight = FontWeight.Medium)
        Box(Modifier.width(1.dp).height(d.dp(24)).background(C.OutlineVariant))
        StarRow(starsFor(time), size = d.dp(20), gap = d.dp(4))
    }
}

@Composable
private fun DialogScrim(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SolvedDialog(
    game: GameState,
    seconds: Int,
    stars: Int,
    puzzle: Int,
    d: Design,
    onNext: () -> Unit,
    onQuit: () -> Unit,
) {
    val next = remember { FocusRequester() }
    LaunchedEffect(Unit) { next.requestFocus() }
    DialogScrim {
        Column(
            modifier = Modifier
                .width(d.dp(560))
                .background(C.SurfaceContainerHigh, RoundedCornerShape(d.dp(28)))
                .padding(d.dp(40)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.dp(20)),
        ) {
            Text(stringResource(R.string.solved_heading, puzzle), color = C.Primary, fontSize = d.sp(16), fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.solved_time, formatTime(seconds)), color = C.OnSurface, fontSize = d.sp(40), fontWeight = FontWeight.Medium)
            StarRow(stars, size = d.dp(64), gap = d.dp(12))
            game.merge?.let {
                Text("${it.result.expr} = $TARGET", color = C.OnSurfaceVariant, fontSize = d.sp(22))
            }
            Row(
                modifier = Modifier.padding(top = d.dp(8)).trapFocus(),
                horizontalArrangement = Arrangement.spacedBy(d.dp(16)),
            ) {
                PillButton(stringResource(R.string.next_puzzle), onNext, d, Modifier.focusRequester(next))
                PillButton(stringResource(R.string.quit_to_title), onQuit, d, tonal = true)
            }
        }
    }
}

@Composable
private fun PauseDialog(
    seconds: Int,
    d: Design,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNewPuzzle: () -> Unit,
    onQuit: () -> Unit,
) {
    val resume = remember { FocusRequester() }
    LaunchedEffect(Unit) { resume.requestFocus() }
    DialogScrim {
        Column(
            modifier = Modifier
                .width(d.dp(440))
                .background(C.SurfaceContainerHigh, RoundedCornerShape(d.dp(28)))
                .padding(start = d.dp(32), end = d.dp(32), top = d.dp(36), bottom = d.dp(28)),
            verticalArrangement = Arrangement.spacedBy(d.dp(12)),
        ) {
            Text(stringResource(R.string.paused), color = C.OnSurface, fontSize = d.sp(32), fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.timer_stopped, formatTime(seconds)),
                color = C.OnSurfaceVariant,
                fontSize = d.sp(16),
                modifier = Modifier.padding(bottom = d.dp(12)),
            )
            Column(Modifier.trapFocus(), verticalArrangement = Arrangement.spacedBy(d.dp(12))) {
                val item = Modifier.fillMaxWidth()
                PillButton(stringResource(R.string.resume), onResume, d, item.focusRequester(resume))
                PillButton(stringResource(R.string.restart_puzzle), onRestart, d, item)
                PillButton(stringResource(R.string.new_puzzle), onNewPuzzle, d, item)
                PillButton(stringResource(R.string.quit_to_title), onQuit, d, item)
            }
            Text(
                stringResource(R.string.pause_help),
                color = C.OnSurfaceVariant,
                fontSize = d.sp(14),
                modifier = Modifier.padding(top = d.dp(8)),
            )
        }
    }
}

@Composable
private fun StarRow(filled: Int, size: Dp, gap: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(3) { i -> StarIcon(filled = i < filled, modifier = Modifier.size(size)) }
    }
}

@Composable
private fun StarIcon(filled: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val outer = size.minDimension / 2
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = -PI / 2 + i * PI / 5
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) drawPath(path, C.Primary)
        else drawPath(path, C.Outline, style = Stroke(width = size.minDimension * 0.07f))
    }
}

@Composable
private fun TimerIcon(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val r = size.minDimension * 0.36f
        val center = Offset(size.width / 2, size.height * 0.56f)
        drawCircle(C.OnSurfaceVariant, radius = r, center = center, style = Stroke(stroke))
        drawLine(C.OnSurfaceVariant, center, center.copy(y = center.y - r * 0.6f), stroke, StrokeCap.Round)
        drawLine(
            C.OnSurfaceVariant,
            Offset(size.width * 0.38f, size.height * 0.08f),
            Offset(size.width * 0.62f, size.height * 0.08f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun DpadIcon(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 52f
        val cross = Path().apply {
            moveTo(19 * s, 4 * s)
            lineTo(33 * s, 4 * s); lineTo(33 * s, 19 * s); lineTo(48 * s, 19 * s)
            lineTo(48 * s, 33 * s); lineTo(33 * s, 33 * s); lineTo(33 * s, 48 * s)
            lineTo(19 * s, 48 * s); lineTo(19 * s, 33 * s); lineTo(4 * s, 33 * s)
            lineTo(4 * s, 19 * s); lineTo(19 * s, 19 * s)
            close()
        }
        drawPath(cross, C.Outline, style = Stroke(width = 2 * s))
        drawCircle(C.Primary, radius = 5 * s, center = Offset(26 * s, 26 * s))
    }
}

private fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp) = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 4, w * 3))),
    )
}

private fun formatTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

@Preview(device = Devices.TV_1080p)
@Composable
private fun GameBoardPreview() {
    val game = GameState.new(listOf(3, 4, 5, 2)).press(Slot.UP).press(Slot.UP)
    DesignCanvas { d -> GameBoard(game, seconds = MutableStateFlow(8), puzzle = 12, difficulty = Difficulty.MEDIUM, d = d) }
}
