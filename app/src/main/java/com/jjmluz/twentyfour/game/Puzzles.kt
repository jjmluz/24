package com.jjmluz.twentyfour.game

import org.json.JSONObject
import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

/** What the player picks on the title screen. */
enum class DifficultyChoice(val difficulty: Difficulty?) {
    ALL(null),
    EASY(Difficulty.EASY),
    MEDIUM(Difficulty.MEDIUM),
    HARD(Difficulty.HARD),
}

data class Puzzle(val numbers: List<Int>, val difficulty: Difficulty)

/** The solvable puzzles from solutions.json. Numbers in a stored puzzle are sorted. */
class PuzzleDatabase(puzzles: List<Puzzle>) {

    private val byDifficulty = puzzles.groupBy { it.difficulty }

    /**
     * A random puzzle other than [previous], with its numbers shuffled so they aren't dealt in order.
     * [DifficultyChoice.ALL] picks a level first: about 70% of the database is hard, and
     * choosing evenly across all puzzles would rarely deal an easy one.
     */
    fun next(choice: DifficultyChoice, previous: Puzzle? = null, random: Random = Random.Default): Puzzle {
        val level = choice.difficulty ?: byDifficulty.keys.random(random)
        val pool = byDifficulty.getValue(level)
        val puzzle = pool.filter { it.numbers != previous?.numbers?.sorted() }.ifEmpty { pool }.random(random)
        return puzzle.copy(numbers = puzzle.numbers.shuffled(random))
    }

    fun count(difficulty: Difficulty): Int = byDifficulty[difficulty]?.size ?: 0

    companion object {
        /** Unsolvable combinations are left out, so they can never be dealt. */
        fun parse(json: String): PuzzleDatabase {
            val combos = JSONObject(json).getJSONArray("combos")
            val puzzles = (0 until combos.length())
                .map(combos::getJSONObject)
                .filter { it.getBoolean("solvable") }
                .map { combo ->
                    val numbers = combo.getJSONArray("numbers")
                    Puzzle(
                        numbers = List(numbers.length(), numbers::getInt),
                        difficulty = difficultyOf(combo.getString("difficulty")),
                    )
                }
            return PuzzleDatabase(puzzles)
        }

        // The file's "expert" level (fractions required) counts as Difícil: the game has three levels.
        private fun difficultyOf(name: String) = when (name) {
            "easy" -> Difficulty.EASY
            "medium" -> Difficulty.MEDIUM
            "hard", "expert" -> Difficulty.HARD
            else -> error("Unknown difficulty: $name")
        }
    }
}
