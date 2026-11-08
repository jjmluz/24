package com.jjmluz.twentyfour.game

const val TARGET = 24

/** The four D-pad directions. Each one maps to a number slot and an operator. */
enum class Slot { UP, LEFT, RIGHT, DOWN }

enum class Op(val symbol: String, val precedence: Int) {
    PLUS("+", 1), MINUS("−", 1), TIMES("×", 2), DIVIDE("÷", 2);

    fun apply(a: Fraction, b: Fraction): Fraction? = when (this) {
        PLUS -> a + b
        MINUS -> a - b
        TIMES -> a * b
        DIVIDE -> a.divOrNull(b)
    }

    companion object {
        /** + up top, − down below, ÷ on the left, × on the right. */
        fun forSlot(slot: Slot) = when (slot) {
            Slot.UP -> PLUS
            Slot.DOWN -> MINUS
            Slot.LEFT -> DIVIDE
            Slot.RIGHT -> TIMES
        }
    }
}

/**
 * A number on the board. [expr] is how it was built ("3" or "3 + 5"),
 * which lets the solved screen show the full solution. [op] is the
 * expression's outermost operator, used to add only the brackets needed.
 */
data class Card(val value: Fraction, val expr: String, val op: Op? = null) {

    val isResult: Boolean get() = op != null

    companion object {
        fun digit(d: Int) = Card(Fraction.of(d), "$d")

        fun combine(a: Card, op: Op, b: Card): Card? = op.apply(a.value, b.value)?.let {
            val left = if (a.op != null && a.op.precedence < op.precedence) "(${a.expr})" else a.expr
            // a − (b + c) and a ÷ (b × c) need brackets even at equal precedence.
            val rightNeedsBrackets = b.op != null &&
                (b.op.precedence < op.precedence ||
                    (b.op.precedence == op.precedence && (op == Op.MINUS || op == Op.DIVIDE)))
            val right = if (rightNeedsBrackets) "(${b.expr})" else b.expr
            Card(it, "$left ${op.symbol} $right", op)
        }
    }
}

/** Faster solves earn more stars: 3 within 30 s, 2 within 60 s, otherwise 1. */
fun starsFor(seconds: Int) = when {
    seconds <= 30 -> 3
    seconds <= 60 -> 2
    else -> 1
}
