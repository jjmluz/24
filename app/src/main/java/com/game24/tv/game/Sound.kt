package com.game24.tv.game

enum class Sound { DEAL, PICK_NUMBER, PICK_OP, MERGE, UNDO, BLOCKED, MISS, SOLVED }

/** What a D-pad press sounds like. Presses are silent while the board is busy or finished. */
fun pressSound(before: GameState, after: GameState): Sound? = when {
    before.phase != Phase.PICK_FIRST && before.phase != Phase.PICK_OP && before.phase != Phase.PICK_SECOND -> null
    after === before || after.divideByZero -> Sound.BLOCKED
    after.phase == Phase.PICK_SECOND -> Sound.PICK_OP
    else -> Sound.PICK_NUMBER
}

/** What a merge sounds like as its result lands on the board. */
fun landingSound(state: GameState): Sound = when (state.phase) {
    Phase.SOLVED -> Sound.SOLVED
    Phase.MISS -> Sound.MISS
    else -> Sound.MERGE
}
