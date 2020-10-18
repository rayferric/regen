package com.rayferric.regen.reverser;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A general purpose implementation of a linear congruential generator based random number generator (LCG-RNG).
 */
public class Random {
    /**
     * Constructs an RNG. The initial seed must be {@link #scramble() scrambled} manually.
     *
     * @param lcg  the LCG to use
     * @param seed initial seed
     */
    public Random(@NotNull LCG lcg, long seed) {
        this.lcg = lcg;
        setSeed(seed);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Random random = (Random)o;
        return seed == random.seed &&
                Objects.equals(lcg, random.lcg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lcg, seed);
    }

    @Override
    public String toString() {
        return String.format("Random{lcg=%s, seed=%s}", lcg, seed);
    }

    /**
     * Scrambles the internal seed of this RNG using a specified LCG.
     * Scrambling two times with the same LCG goes back and forth.
     *
     * @param lcg the LCG to use
     */
    public void scramble(@NotNull LCG lcg) {
        seed = lcg.scramble(seed);
    }

    /**
     * Scrambles the internal seed of this RNG using its internal LCG.
     * Scrambling two times with the same LCG goes back and forth.
     */
    public void scramble() {
        scramble(lcg);
    }

    /**
     * Skips single {@link #nextSeed()} call. (See also: {@link #skip(int)}.)
     */
    public void skip() {
        seed = lcg.next(seed);
    }

    /**
     * Skips multiple {@link #nextSeed()} calls. See the {@link SeedCall#SKIPS}
     * field of any call type to know how many steps to execute.
     */
    public void skip(int step) {
        seed = lcg.ofStep(step).next(seed);
    }

    /**
     * Returns the internal LCG of this RNG.
     *
     * @return the current LCG
     */
    public LCG getLcg() {
        return lcg;
    }

    /**
     * Sets the internal LCG of this RNG.
     *
     * @param lcg the new LCG
     */
    public void setLcg(@NotNull LCG lcg) {
        this.lcg = lcg;
    }

    /**
     * Returns the current internal seed for this RNG.
     *
     * @return seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Sets the current internal seed for this RNG.
     *
     * @param seed the new seed
     */
    public void setSeed(long seed) {
        this.seed = lcg.mod(seed);
    }

    /**
     * Advances the RNG one-step further and returns the new seed.
     *
     * @return seed
     */
    public long nextSeed() {
        skip();
        return seed;
    }

    protected LCG lcg;
    protected long seed;
}