package com.game24.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Screen { TITLE, GAME }

/** Two screens, so plain state is enough for navigation. Back on the title screen exits the app. */
@Composable
fun Game24App(vm: GameViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.TITLE) }
    val difficultyChoice by vm.difficultyChoice.collectAsStateWithLifecycle()
    when (screen) {
        Screen.TITLE -> TitleScreen(
            choice = difficultyChoice,
            onChoose = vm::chooseDifficulty,
            onPlay = {
                vm.start()
                screen = Screen.GAME
            },
        )
        Screen.GAME -> GameScreen(
            vm = vm,
            onQuit = {
                vm.quit()
                screen = Screen.TITLE
            },
        )
    }
}
