package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.LCG;
import com.rayferric.regen.reverser.Random;
import org.jetbrains.annotations.NotNull;

/**
 * All purpose implementation of the Java RNG. Provides easy access to the internals of the generator.
 */
public class JavaRandom extends Random {
    /**
     * Constructs Java RNG with an initial seed.
     * The seed must be scrambled manually.
     *
     * @param seed the initial seed
     */
    public JavaRandom(long seed) {
        super(LCG.JAVA, seed);
    }

    /**
     * Generates a boolean. Consumes one seed call.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static boolean nextBoolean(@NotNull Random random) {
        return next(random, 1) != 0;
    }

    /**
     * Generates an integer. Consumes one seed call.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static int nextInt(@NotNull Random random) {
        return next(random, 32);
    }

    /**
     * Generates a ranged integer. Consumes at least one seed call with
     * a rare chance for a second one if range is not a power of two.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static int nextInt(@NotNull Random random, int range) {
        if(range <= 0)
            throw new IllegalArgumentException("Range must be positive.");

        // When range is a power of 2:
        if((range & -range) == range)
            return (int)((range * (long)next(random, 31)) >> 31);

        int bits, value;
        do {
            bits = next(random, 31);
            value = bits % range;
        } while(bits - value + (range - 1) < 0);
        return value;
    }

    /**
     * Generates a long. Consumes two seed calls.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static long nextLong(@NotNull Random random) {
        return ((long)next(random, 32) << 32) + next(random, 32);
    }

    /**
     * Generates a float. Consumes one seed call.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static float nextFloat(@NotNull Random random) {
        return next(random, 24) / ((float)(1 << 24));
    }

    /**
     * Generates a double. Consumes two seed calls.
     *
     * @param random RNG to use
     *
     * @return generated value
     */
    public static double nextDouble(@NotNull Random random) {
        return (((long)next(random, 26) << 27) + next(random, 27)) / (double)(1L << 53);
    }

    /**
     * Generates a boolean. Consumes one seed call.
     *
     * @return generated value
     */
    public boolean nextBoolean() {
        return nextBoolean(this);
    }

    /**
     * Generates an integer. Consumes one seed call.
     *
     * @return generated value
     */
    public int nextInt() {
        return nextInt(this);
    }

    /**
     * Generates a ranged integer. Consumes at least one seed call with
     * a rare chance for a second one if range is not a power of two.
     *
     * @return generated value
     */
    public int nextInt(int range) {
        return nextInt(this, range);
    }

    /**
     * Generates a long. Consumes two seed calls.
     *
     * @return generated value
     */
    public long nextLong() {
        return nextLong(this);
    }

    /**
     * Generates a float. Consumes one seed call.
     *
     * @return generated value
     */
    public float nextFloat() {
        return nextFloat(this);
    }

    /**
     * Generates a double. Consumes two seed calls.
     *
     * @return generated value
     */
    public double nextDouble() {
        return nextDouble(this);
    }

    // Outputs at most 48 bits per call:
    private static int next(@NotNull Random random, int bits) {
        return (int)(random.nextSeed() >>> (48 - bits));
    }
}