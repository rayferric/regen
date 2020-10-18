package com.rayferric.regen.reverser;

import java.util.Objects;

/**
 * Immutable linear congruential generator (LCG) object.
 */
public class LCG {
    public static final LCG JAVA = new LCG(0x5DEECE66DL, 0xBL, 1L << 48);

    /**
     * Constructs a custom LCG.
     *
     * <p>A linear congruential generator can be denoted as the following function:
     *
     * <p><b>y = ax + b mod m</b>
     *
     * @param multiplier the multiplier (<b>a</b>)
     * @param addend     the addend (<b>b</b>)
     * @param modulus    the modulus (<b>m</b>)
     */
    public LCG(long multiplier, long addend, long modulus) {
        this.multiplier = multiplier;
        this.addend = addend;
        this.modulus = modulus;

        // True for when the modulus is a power of two:
        canMask = (modulus & -modulus) == modulus;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        LCG lcg = (LCG)o;
        return multiplier == lcg.multiplier &&
                addend == lcg.addend &&
                modulus == lcg.modulus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiplier, addend, modulus);
    }

    @Override
    public String toString() {
        return String.format("LCG{multiplier=%s, addend=%s, modulus=%s}", multiplier, addend, modulus);
    }

    /**
     * Combines multiple steps of this LCG into a single one that can skip several updates in a single cycle.
     *
     * @param step the number of steps
     *
     * @return a new, combined LCG
     */

    public LCG ofStep(int step) {
        if(step == 0) return new LCG(1, 0, modulus);
        if(step == 1) return this;

        // Not used due to integer overflow:
        // long multiplier = pow(other.multiplier, steps);
        // long addend = other.addend * ((multiplier - 1) / (other.multiplier - 1));

        long baseMultiplier = multiplier;
        long baseAddend = addend;

        long multiplier = 1;
        long addend = 0;

        for(long i = step; i != 0; i >>>= 1) {
            if((i & 1) != 0) {
                multiplier *= baseMultiplier;
                addend *= baseMultiplier;
                addend += baseAddend;
            }

            baseAddend *= (baseMultiplier + 1);
            baseMultiplier *= baseMultiplier;
        }

        return new LCG(multiplier, addend, modulus);
    }

    /**
     * Applies the modulus of this LCG to the given seed.
     *
     * @param seed a number
     *
     * @return seed mod <b>m</b>
     */
    public long mod(long seed) {
        if(canMask)
            return seed & (modulus - 1);
        else
            return seed % modulus;
    }

    /**
     * Scrambles a seed using XOR operator.
     *
     * <p>Calling this two consecutive times on a single seed cycles back and forth.
     *
     * @param seed a seed to scramble
     *
     * @return scrambled seed
     */
    public long scramble(long seed) {
        return mod(seed ^ multiplier);
    }

    /**
     * Returns the next seed in sequence after the given one.
     *
     * @param seed a seed
     *
     * @return next seed
     */
    public long next(long seed) {
        return mod(seed * multiplier + addend);
    }

    /**
     * Returns the multiplier (<b>a</b>) of this LCG.
     *
     * @return multiplier
     */
    public long getMultiplier() {
        return multiplier;
    }

    /**
     * Returns the addend (<b>b</b>) of this LCG.
     *
     * @return addend
     */
    public long getAddend() {
        return addend;
    }

    /**
     * Returns the modulus (<b>m</b>) of this LCG.
     *
     * @return modulus
     */
    public long getModulus() {
        return modulus;
    }

    private final long multiplier, addend, modulus;
    private final boolean canMask;
}
