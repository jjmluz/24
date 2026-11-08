package com.jjmluz.twentyfour.game

enum class Phase {
    /** Waiting for the first number of a step. */
    PICK_FIRST,

    /** First number is in the equation row; waiting for an operator. */
    PICK_OP,

    /** "a op" is in the equation row; waiting for the second number. */
    PICK_SECOND,

    /** "a op b = r" is shown; input is paused until [GameState.completeMerge]. */
    MERGING,

    /** Last card isn't 24; the board resets after a short pause via [GameState.resetAfterMiss]. */
    MISS,

    /** Last card is 24. */
    SOLVED,
}

data class Merge(val first: Card, val op: Op, val second: Card, val result: Card, val targetSlot: Slot)

/**
 * Immutable game state. Every input returns a new state, which keeps the rules
 * testable without Android.
 *
 * The first picked card stays in [board] and is drawn as an empty slot while
 * [firstSlot] points at it. A merge writes its result into the second card's slot.
 */
data class GameState(
    val start: Map<Slot, Card>,
    val board: Map<Slot, Card?>,
    val phase: Phase = Phase.PICK_FIRST,
    val firstSlot: Slot? = null,
    val op: Op? = null,
    val merge: Merge? = null,
    val history: List<Map<Slot, Card?>> = emptyList(),
    /** Set when the last press tried to divide by zero, which the rules don't allow. */
    val divideByZero: Boolean = false,
) {
    val firstCard: Card? get() = firstSlot?.let { board[it] }

    /** Cards the player can still see: picked cards have moved down to the equation row. */
    fun visibleCard(slot: Slot): Card? = when {
        slot == firstSlot -> null
        phase == Phase.MERGING && slot == merge?.targetSlot -> null
        else -> board[slot]
    }

    val canUndo: Boolean
        get() = when (phase) {
            Phase.PICK_OP, Phase.PICK_SECOND -> true
            Phase.PICK_FIRST -> history.isNotEmpty()
            Phase.MERGING, Phase.MISS, Phase.SOLVED -> false
        }

    fun press(slot: Slot): GameState = when (phase) {
        Phase.PICK_FIRST ->
            if (board[slot] == null) this
            else copy(phase = Phase.PICK_OP, firstSlot = slot, divideByZero = false)

        Phase.PICK_OP -> copy(phase = Phase.PICK_SECOND, op = Op.forSlot(slot), divideByZero = false)

        Phase.PICK_SECOND -> {
            val first = firstCard
            val second = visibleCard(slot)
            val op = op
            if (first == null || second == null || op == null) {
                this
            } else {
                val result = Card.combine(first, op, second)
                if (result == null) copy(divideByZero = true)
                else copy(phase = Phase.MERGING, merge = Merge(first, op, second, result, slot), divideByZero = false)
            }
        }

        Phase.MERGING, Phase.MISS, Phase.SOLVED -> this
    }

    /** Puts the merge result into the second card's slot and checks for the end of the puzzle. */
    fun completeMerge(): GameState {
        val m = merge ?: return this
        if (phase != Phase.MERGING) return this
        val newBoard = board.toMutableMap().apply {
            put(firstSlot!!, null)
            put(m.targetSlot, m.result)
        }
        val remaining = newBoard.values.count { it != null }
        val nextPhase = when {
            remaining > 1 -> Phase.PICK_FIRST
            m.result.value == Fraction.of(TARGET) -> Phase.SOLVED
            else -> Phase.MISS
        }
        return copy(
            board = newBoard,
            phase = nextPhase,
            firstSlot = null,
            op = null,
            merge = if (nextPhase == Phase.PICK_FIRST) null else m,
            history = history + listOf(board),
        )
    }

    fun resetAfterMiss(): GameState = fresh(start)

    /** Steps back one input. Returns this state unchanged when there's nothing to undo. */
    fun undo(): GameState = when {
        !canUndo -> this
        phase == Phase.PICK_SECOND -> copy(phase = Phase.PICK_OP, op = null, divideByZero = false)
        phase == Phase.PICK_OP -> copy(phase = Phase.PICK_FIRST, firstSlot = null, divideByZero = false)
        else -> copy(board = history.last(), history = history.dropLast(1), divideByZero = false)
    }

    companion object {
        fun new(digits: List<Int>): GameState {
            require(digits.size == Slot.entries.size)
            return fresh(Slot.entries.zip(digits.map(Card::digit)).toMap())
        }

        private fun fresh(start: Map<Slot, Card>) = GameState(start = start, board = start)
    }
}
