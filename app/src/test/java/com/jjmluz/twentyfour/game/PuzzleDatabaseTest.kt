package com.jjmluz.twentyfour.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class PuzzleDatabaseTest {
    private val json = File("../solutions.json").readText()
    private val database = PuzzleDatabase.parse(json)

    @Test fun leavesOutUnsolvablePuzzles() {
        assertEquals(216, database.count(Difficulty.EASY))
        assertEquals(181, database.count(Difficulty.MEDIUM))
        // 949 hard + 16 expert
        assertEquals(965, database.count(Difficulty.HARD))
    }

    @Test fun everyPuzzleOfEveryLevelCanBeDealtAndSolved() {
        val random = Random(7)
        DifficultyChoice.entries.forEach { choice ->
            repeat(300) {
                val puzzle = database.next(choice, random = random)
                assertEquals(4, puzzle.numbers.size)
                assertTrue(puzzle.numbers.all { it in 1..13 })
                assertNotNull(puzzle.numbers.toString(), Solver.solve(puzzle.numbers))
            }
        }
    }

    @Test fun dealsOnlyTheChosenLevel() {
        val random = Random(1)
        listOf(DifficultyChoice.EASY, DifficultyChoice.MEDIUM, DifficultyChoice.HARD).forEach { choice ->
            repeat(200) { assertEquals(choice.difficulty, database.next(choice, random = random).difficulty) }
        }
    }

    @Test fun allMixesEveryLevelEvenly() {
        val random = Random(2)
        val counts = List(3000) { database.next(DifficultyChoice.ALL, random = random).difficulty }
            .groupingBy { it }.eachCount()
        Difficulty.entries.forEach { assertTrue("$it: $counts", counts.getValue(it) in 850..1150) }
    }

    @Test fun numbersAreShuffled() {
        val random = Random(3)
        val puzzles = List(100) { database.next(DifficultyChoice.EASY, random = random).numbers }
        assertTrue(puzzles.any { it != it.sorted() })
    }

    @Test fun neverDealsTheSamePuzzleTwiceInARow() {
        val random = Random(4)
        val two = PuzzleDatabase(
            listOf(Puzzle(listOf(1, 2, 3, 4), Difficulty.EASY), Puzzle(listOf(2, 3, 4, 5), Difficulty.EASY)),
        )
        var previous = two.next(DifficultyChoice.EASY, random = random)
        repeat(50) {
            val next = two.next(DifficultyChoice.EASY, previous = previous, random = random)
            assertNotEquals(previous.numbers.sorted(), next.numbers.sorted())
            previous = next
        }
    }

    @Test fun solverAgreesWithUnsolvableEntries() {
        assertNull(Solver.solve(listOf(1, 1, 1, 1)))
        assertNull(Solver.solve(listOf(13, 13, 13, 13)))
        assertTrue("\"combo\":\"1-1-1-1\",\"numbers\":[1,1,1,1],\"solvable\":false" in json)
    }
}
