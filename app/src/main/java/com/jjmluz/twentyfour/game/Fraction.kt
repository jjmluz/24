package com.jjmluz.twentyfour.game

import kotlin.math.abs

/** Exact rational number, always stored in lowest terms with a positive denominator. */
class Fraction private constructor(val num: Long, val den: Long) {

    val isZero: Boolean get() = num == 0L

    operator fun plus(other: Fraction) = of(num * other.den + other.num * den, den * other.den)
    operator fun minus(other: Fraction) = of(num * other.den - other.num * den, den * other.den)
    operator fun times(other: Fraction) = of(num * other.num, den * other.den)

    /** Returns null when dividing by zero, which the rules don't allow. */
    fun divOrNull(other: Fraction): Fraction? =
        if (other.isZero) null else of(num * other.den, den * other.num)

    override fun equals(other: Any?) = other is Fraction && num == other.num && den == other.den
    override fun hashCode() = 31 * num.hashCode() + den.hashCode()

    /** "8", "8/3", "−1/3" — uses a real minus sign for display. */
    override fun toString(): String {
        val sign = if (num < 0) "−" else ""
        return if (den == 1L) "$sign${abs(num)}" else "$sign${abs(num)}/$den"
    }

    companion object {
        fun of(n: Long, d: Long = 1L): Fraction {
            require(d != 0L) { "Denominator must not be zero" }
            val g = gcd(abs(n), abs(d))
            val s = if (d < 0) -1 else 1
            return Fraction(s * n / g, s * d / g)
        }

        fun of(n: Int) = of(n.toLong())

        private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}
