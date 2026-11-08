package com.jjmluz.twentyfour.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Independent check of the rules and of solutions.json. Returns one solution, or null when there is none. */
object Solver {
    fun solve(digits: List<Int>): String? = search(digits.map(Card::digit), Fraction.of(TARGET))?.expr

    private fun search(cards: List<Card>, target: Fraction): Card? {
        if (cards.size == 1) return cards[0].takeIf { it.value == target }
        for (i in cards.indices) for (j in cards.indices) {
            if (i == j) continue
            val rest = cards.filterIndexed { k, _ -> k != i && k != j }
            for (op in Op.entries) {
                // + and × are commutative: trying one order is enough.
                if (j < i && (op == Op.PLUS || op == Op.TIMES)) continue
                val merged = Card.combine(cards[i], op, cards[j]) ?: continue
                search(rest + merged, target)?.let { return it }
            }
        }
        return null
    }
}

class FractionTest {
    @Test fun normalizes() {
        assertEquals(Fraction.of(2, 3), Fraction.of(4, 6))
        assertEquals(Fraction.of(-1, 2), Fraction.of(1, -2))
        assertEquals("8/3", Fraction.of(8, 3).toString())
        assertEquals("−1/3", (Fraction.of(8, 3) - Fraction.of(3)).toString())
        assertEquals("0", (Fraction.of(5) - Fraction.of(5)).toString())
    }

    @Test fun divisionByZeroIsNull() {
        assertNull(Fraction.of(3).divOrNull(Fraction.of(0)))
    }
}

class SolverTest {
    @Test fun findsSimpleSolution() {
        assertNotNull(Solver.solve(listOf(3, 4, 5, 2)))
    }

    @Test fun needsFractions() {
        // Only solution: 8 ÷ (3 − 8 ÷ 3)
        assertNotNull(Solver.solve(listOf(3, 3, 8, 8)))
        assertNotNull(Solver.solve(listOf(1, 5, 5, 5)))
    }

    @Test fun reportsUnsolvable() {
        assertNull(Solver.solve(listOf(1, 1, 1, 1)))
        assertNull(Solver.solve(listOf(9, 9, 9, 9)))
    }

    @Test fun stars() {
        assertEquals(3, starsFor(30))
        assertEquals(2, starsFor(31))
        assertEquals(2, starsFor(60))
        assertEquals(1, starsFor(61))
    }
}

class GameStateTest {
    // Board: 3 up, 4 left, 5 right, 2 down. Operators: + up, − down, ÷ left, × right.
    private val start = GameState.new(listOf(3, 4, 5, 2))

    private fun GameState.step(a: Slot, op: Slot, b: Slot) = press(a).press(op).press(b).completeMerge()

    @Test fun pickingANumberLiftsItOffTheBoard() {
        val s = start.press(Slot.UP)
        assertEquals(Phase.PICK_OP, s.phase)
        assertNull(s.visibleCard(Slot.UP))
        assertEquals(Fraction.of(3), s.firstCard?.value)
    }

    @Test fun mergeResultLandsInSecondSlot() {
        val merging = start.press(Slot.UP).press(Slot.UP).press(Slot.RIGHT)
        assertEquals(Phase.MERGING, merging.phase)
        assertNull(merging.visibleCard(Slot.RIGHT))

        val s = merging.completeMerge()
        assertEquals(Phase.PICK_FIRST, s.phase)
        assertNull(s.board[Slot.UP])
        val result = s.board[Slot.RIGHT]!!
        assertEquals(Fraction.of(8), result.value)
        assertTrue(result.isResult)
        assertEquals("3 + 5", result.expr)
    }

    @Test fun emptyAndSameSlotsAreIgnored() {
        val afterMerge = start.step(Slot.UP, Slot.UP, Slot.RIGHT)
        assertSame(afterMerge, afterMerge.press(Slot.UP))

        val waiting = start.press(Slot.LEFT).press(Slot.UP)
        assertSame(waiting, waiting.press(Slot.LEFT))
    }

    @Test fun solvesWithChainedResults() {
        // 3 + 5 = 8 → 8 − 2 = 6 → 6 × 4 = 24
        val s = start
            .step(Slot.UP, Slot.UP, Slot.RIGHT)
            .step(Slot.RIGHT, Slot.DOWN, Slot.DOWN)
            .step(Slot.DOWN, Slot.RIGHT, Slot.LEFT)
        assertEquals(Phase.SOLVED, s.phase)
        assertEquals(Fraction.of(24), s.board[Slot.LEFT]?.value)
        assertEquals("(3 + 5 − 2) × 4", s.board[Slot.LEFT]?.expr)
    }

    @Test fun bracketsOnlyWhereNeeded() {
        val three = Card.digit(3)
        val eight = Card.digit(8)
        val eightThirds = Card.combine(eight, Op.DIVIDE, three)!!
        val oneThird = Card.combine(three, Op.MINUS, eightThirds)!!
        assertEquals("3 − 8 ÷ 3", oneThird.expr)
        val answer = Card.combine(eight, Op.DIVIDE, oneThird)!!
        assertEquals("8 ÷ (3 − 8 ÷ 3)", answer.expr)
        assertEquals(Fraction.of(24), answer.value)
    }

    @Test fun solvesWithTwoSeparatePairs() {
        // (6 − 2) × (8 − 2): the first result waits on the board while the second pair is built.
        val s = GameState.new(listOf(6, 2, 2, 8))
            .step(Slot.UP, Slot.DOWN, Slot.LEFT) // 6 − 2 = 4 in LEFT
            .step(Slot.DOWN, Slot.DOWN, Slot.RIGHT) // 8 − 2 = 6 in RIGHT
            .step(Slot.LEFT, Slot.RIGHT, Slot.RIGHT) // 4 × 6 = 24
        assertEquals(Phase.SOLVED, s.phase)
        assertEquals("(6 − 2) × (8 − 2)", s.merge?.result?.expr)
    }

    @Test fun missResetsToStartingCards() {
        val s = start
            .step(Slot.UP, Slot.UP, Slot.RIGHT)
            .step(Slot.RIGHT, Slot.RIGHT, Slot.LEFT)
            .step(Slot.LEFT, Slot.DOWN, Slot.DOWN) // 32 − 2 = 30
        assertEquals(Phase.MISS, s.phase)
        val reset = s.resetAfterMiss()
        assertEquals(Phase.PICK_FIRST, reset.phase)
        assertEquals(start.board, reset.board)
        assertTrue(reset.history.isEmpty())
    }

    @Test fun divideByZeroIsBlocked() {
        val zero = GameState.new(listOf(5, 5, 3, 1)).step(Slot.UP, Slot.DOWN, Slot.LEFT) // 5 − 5 = 0 in LEFT
        val s = zero.press(Slot.RIGHT).press(Slot.LEFT).press(Slot.LEFT) // 3 ÷ 0
        assertEquals(Phase.PICK_SECOND, s.phase)
        assertTrue(s.divideByZero)
    }

    @Test fun undoStepsBackOneInputAtATime() {
        val waitingSecond = start.press(Slot.UP).press(Slot.UP)
        assertEquals(Phase.PICK_OP, waitingSecond.undo().phase)
        assertEquals(Phase.PICK_FIRST, waitingSecond.undo().undo().phase)
        assertFalse(start.canUndo)

        val merged = start.step(Slot.UP, Slot.UP, Slot.RIGHT)
        assertTrue(merged.canUndo)
        assertEquals(start.board, merged.undo().board)
    }
}

class SoundTest {
    private val start = GameState.new(listOf(3, 0, 8, 1).map { if (it == 0) 2 else it })

    @org.junit.Test
    fun `presses sound by what they picked`() {
        val number = start.press(Slot.UP)
        val op = number.press(Slot.UP)
        org.junit.Assert.assertEquals(Sound.PICK_NUMBER, pressSound(start, number))
        org.junit.Assert.assertEquals(Sound.PICK_OP, pressSound(number, op))
        org.junit.Assert.assertEquals(Sound.PICK_NUMBER, pressSound(op, op.press(Slot.LEFT)))
    }

    @org.junit.Test
    fun `picking the empty slot is blocked`() {
        val picking = start.press(Slot.UP).press(Slot.UP)
        org.junit.Assert.assertEquals(Sound.BLOCKED, pressSound(picking, picking.press(Slot.UP)))
    }

    @org.junit.Test
    fun `presses are silent during a merge`() {
        val merging = start.press(Slot.UP).press(Slot.UP).press(Slot.LEFT)
        org.junit.Assert.assertNull(pressSound(merging, merging.press(Slot.DOWN)))
    }

    @org.junit.Test
    fun `landing sound depends on the outcome`() {
        val merging = start.press(Slot.UP).press(Slot.UP).press(Slot.LEFT)
        org.junit.Assert.assertEquals(Sound.MERGE, landingSound(merging.completeMerge()))
    }
}
