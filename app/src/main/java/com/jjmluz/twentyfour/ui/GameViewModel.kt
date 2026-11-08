package com.jjmluz.twentyfour.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jjmluz.twentyfour.game.Difficulty
import com.jjmluz.twentyfour.game.DifficultyChoice
import com.jjmluz.twentyfour.game.GameState
import com.jjmluz.twentyfour.game.Phase
import com.jjmluz.twentyfour.game.Puzzle
import com.jjmluz.twentyfour.game.PuzzleDatabase
import com.jjmluz.twentyfour.game.Slot
import com.jjmluz.twentyfour.game.Sound
import com.jjmluz.twentyfour.game.landingSound
import com.jjmluz.twentyfour.game.pressSound
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** How long "a op b = r" stays in the equation row before r drops into its slot. */
private const val MERGE_DELAY_MS = 600L

/** How long a wrong final result stays on screen before the cards come back. */
private const val MISS_DELAY_MS = 1400L

private const val PREFERENCES = "game24"
private const val KEY_DIFFICULTY = "difficulty"

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val sounds = GameSounds(application)

    private val preferences = application.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val database = PuzzleDatabase.parse(
        application.assets.open("solutions.json").bufferedReader().use { it.readText() },
    )

    private val _difficultyChoice = MutableStateFlow(
        DifficultyChoice.entries.find { it.name == preferences.getString(KEY_DIFFICULTY, null) }
            ?: DifficultyChoice.ALL,
    )
    val difficultyChoice: StateFlow<DifficultyChoice> = _difficultyChoice.asStateFlow()

    private var currentPuzzle: Puzzle = database.next(_difficultyChoice.value)

    private val _game = MutableStateFlow(GameState.new(currentPuzzle.numbers))
    val game: StateFlow<GameState> = _game.asStateFlow()

    private val _difficulty = MutableStateFlow(currentPuzzle.difficulty)

    /** Difficulty of the puzzle on the board. */
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _seconds = MutableStateFlow(0)
    val seconds: StateFlow<Int> = _seconds.asStateFlow()

    private val _puzzle = MutableStateFlow(0)
    val puzzle: StateFlow<Int> = _puzzle.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private var timerJob: Job? = null
    private var mergeJob: Job? = null

    fun chooseDifficulty(choice: DifficultyChoice) {
        _difficultyChoice.value = choice
        preferences.edit { putString(KEY_DIFFICULTY, choice.name) }
    }

    /** Called from the title screen. */
    fun start() = dealNext()

    fun onDirection(slot: Slot) {
        if (_paused.value) return
        val before = _game.value
        val after = before.press(slot)
        pressSound(before, after)?.let(sounds::play)
        if (after === before) return
        _game.value = after
        if (after.phase == Phase.MERGING) finishMerge()
    }

    fun onBack() {
        if (_game.value.canUndo) sounds.play(Sound.UNDO)
        _game.update { it.undo() }
    }

    fun nextPuzzle() = dealNext()

    fun pause() {
        if (_paused.value || _game.value.phase == Phase.SOLVED) return
        _paused.value = true
        stopTimer()
    }

    fun resume() {
        if (!_paused.value) return
        _paused.value = false
        startTimer()
    }

    /** Same four numbers again. The clock keeps its time so restarting can't earn extra stars. */
    fun restartPuzzle() {
        mergeJob?.cancel()
        _game.update { it.resetAfterMiss() }
        resume()
    }

    /** Skips to a new puzzle without stars. */
    fun newPuzzle() = dealNext()

    fun quit() {
        mergeJob?.cancel()
        stopTimer()
        _paused.value = false
    }

    override fun onCleared() = sounds.release()

    private fun finishMerge() {
        mergeJob = viewModelScope.launch {
            delay(MERGE_DELAY_MS)
            val merged = _game.value.completeMerge()
            _game.value = merged
            sounds.play(landingSound(merged))
            when (merged.phase) {
                Phase.SOLVED -> stopTimer()
                Phase.MISS -> {
                    delay(MISS_DELAY_MS)
                    _game.update { it.resetAfterMiss() }
                }
                else -> Unit
            }
        }
    }

    private fun dealNext() {
        mergeJob?.cancel()
        currentPuzzle = database.next(_difficultyChoice.value, previous = currentPuzzle)
        _game.value = GameState.new(currentPuzzle.numbers)
        _difficulty.value = currentPuzzle.difficulty
        _puzzle.update { it + 1 }
        _seconds.value = 0
        _paused.value = false
        stopTimer()
        startTimer()
        sounds.play(Sound.DEAL)
    }

    private fun startTimer() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _seconds.update { it + 1 }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
