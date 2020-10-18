package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;

/**
 * Immutable fraction that can represent any rational number.
 */
public class Fraction implements Comparable<Fraction> {
    public static final Fraction ZERO = new Fraction(0);

    public static final Fraction HALF = new Fraction(1, 2);
    public static final Fraction ONE = new Fraction(1);
    public static final Fraction TWO = new Fraction(2);

    public static final Fraction MINUS_HALF = HALF.negate();
    public static final Fraction MINUS_ONE = ONE.negate();
    public static final Fraction MINUS_TWO = TWO.negate();

    /**
     * Constructs an integer number.
     *
     * @param numerator integer value
     */
    public Fraction(long numerator) {
        this(BigInteger.valueOf(numerator));
    }

    /**
     * Constructs a fractional number.
     *
     * @param numerator   multiplication factor
     * @param denominator division factor
     */
    public Fraction(long numerator, long denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    /**
     * Constructs an integer number.
     *
     * @param numerator integer value
     */
    public Fraction(@NotNull BigInteger numerator) {
        this(numerator, BigInteger.ONE);
    }

    /**
     * Constructs a fractional number.
     *
     * @param numerator   multiplication factor
     * @param denominator division factor
     */
    public Fraction(@NotNull BigInteger numerator, @NotNull BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
        simplify();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Fraction fraction = (Fraction)o;
        return Objects.equals(numerator, fraction.numerator) &&
                Objects.equals(denominator, fraction.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        if(isInteger())
            return numerator.toString();
        else
            return String.format("%s/%s", numerator, denominator);
    }

    @Override
    public int compareTo(@NotNull Fraction other) {
        return sub(other).sgn();
    }

    /**
     * Returns a {@link BigDecimal} representation of this number. Removes infinity.
     *
     * @param context math context settings
     *
     * @return a {@link BigDecimal}
     */
    public BigDecimal toBigDecimal(@NotNull MathContext context) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), context);
    }

    /**
     * Returns a double representation of this number. Removes infinity.
     *
     * @return floating-point value
     */
    public double toDouble() {
        return toBigDecimal(MathContext.DECIMAL64).doubleValue();
    }

    /**
     * Adds a fraction to this one and returns a new one.
     *
     * @param other the other addend
     *
     * @return this + other
     */
    public Fraction add(@NotNull Fraction other) {
        BigInteger num1 = numerator.multiply(other.denominator);
        BigInteger num2 = other.numerator.multiply(denominator);
        return new Fraction(num1.add(num2), denominator.multiply(other.denominator));
    }

    /**
     * Subtracts a fraction from this one and returns a new one.
     *
     * @param other the subtrahend
     *
     * @return this - other
     */
    public Fraction sub(@NotNull Fraction other) {
        BigInteger num1 = numerator.multiply(other.denominator);
        BigInteger num2 = other.numerator.multiply(denominator);
        return new Fraction(num1.subtract(num2), denominator.multiply(other.denominator));
    }

    /**
     * Multiplies this fraction by an another one and returns a new one.
     *
     * @param other the other factor
     *
     * @return this * other
     */
    public Fraction mul(@NotNull Fraction other) {
        return new Fraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    /**
     * Divides this fraction by an another one and returns a new one.
     *
     * @param other the divisor
     *
     * @return this / other
     */
    public Fraction div(@NotNull Fraction other) {
        return new Fraction(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    /**
     * Returns a negative of this fraction.
     *
     * @return -this
     */
    public Fraction negate() {
        return new Fraction(numerator.negate(), denominator);
    }

    /**
     * Returns the reciprocal of this fraction.
     *
     * @return 1 / this
     */
    public Fraction inverse() {
        return new Fraction(denominator, numerator);
    }

    /**
     * Returns the greatest integer smaller than this fraction.
     *
     * @return floor(this)
     */
    public Fraction floor() {
        if(denominator.equals(BigInteger.ONE))
            return this;

        if(numerator.signum() == -1)
            return new Fraction(numerator.divide(denominator).subtract(BigInteger.ONE), BigInteger.ONE);

        return new Fraction(numerator.divide(denominator), BigInteger.ONE);
    }

    /**
     * Returns the smallest integer larger than this fraction.
     *
     * @return ceil(this)
     */
    public Fraction ceil() {
        if(denominator.equals(BigInteger.ONE))
            return this;

        if(numerator.signum() == 1)
            return new Fraction(numerator.divide(denominator).add(BigInteger.ONE), BigInteger.ONE);

        return new Fraction(numerator.divide(denominator), BigInteger.ONE);
    }

    /**
     * Returns the approximate integer value of this fraction.
     *
     * @return round(this)
     */
    public Fraction round() {
        return sub(HALF).ceil();
    }

    /**
     * Returns the signum of this fraction.
     *
     * @return sgn(this)
     */
    public int sgn() {
        return numerator.signum();
    }

    /**
     * Returns the absolute value of this fraction.
     *
     * @return |this|
     */
    public Fraction abs() {
        return sgn() == -1 ? negate() : this;
    }

    /**
     * Raises this fraction to a power.
     *
     * @return this<sup>exponent</sup>
     */
    public Fraction pow(int exponent) {
        return new Fraction(numerator.pow(exponent), denominator.pow(exponent));
    }

    /**
     * Returns the remainder after dividing this fraction by another.
     *
     * @return this mod modulus
     */
    public Fraction mod(@NotNull Fraction modulus) {
        return sub(div(modulus).floor().mul(modulus));
    }

    /**
     * Tells whether this fraction is an integer.
     *
     * @return true if integer
     */
    public boolean isInteger() {
        return denominator.equals(BigInteger.ONE);
    }

    /**
     * Returns the numerator (multiplication factor) of this fraction.
     *
     * @return numerator
     */
    public BigInteger getNumerator() {
        return numerator;
    }

    /**
     * Returns the denominator (division factor) of this fraction.
     *
     * @return denominator
     */
    public BigInteger getDenominator() {
        return denominator;
    }

    private BigInteger numerator, denominator;

    private void simplify() {
        // Denominator equals one if the rational value is zero.
        // Only the numerator is signed, denominator is always positive.
        // Both numerator and denominator are co-prime for non-zero numbers.

        if(numerator.signum() == 0) {
            denominator = BigInteger.ONE;
            return;
        }

        if(denominator.signum() == -1) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }

        BigInteger divisor = numerator.gcd(denominator);
        numerator = numerator.divide(divisor);
        denominator = denominator.divide(divisor);
    }
}
