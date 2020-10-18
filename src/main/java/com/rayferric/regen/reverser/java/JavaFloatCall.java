package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextFloat()}.
 */
public class JavaFloatCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static int SKIPS = 1;

    /**
     * Constructs a new float call.
     *
     * @param value value returned by the random call
     */
    public JavaFloatCall(float value) {
        this(value, value);
    }

    /**
     * Constructs a new float call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public JavaFloatCall(float min, float max) {
        this(min, max, false, false);
    }

    /**
     * Constructs a new float call.
     *
     * @param min          minimum value returned by the random call
     * @param max          maximum value returned by the random call
     * @param minExclusive should <b>value == min</b> be disregarded
     * @param maxExclusive should <b>value == max</b> be disregarded
     */
    public JavaFloatCall(float min, float max, boolean minExclusive, boolean maxExclusive) {
        super(SKIPS);
        this.min = minExclusive ? Math.nextUp(min) : min;
        this.max = maxExclusive ? Math.nextDown(max) : max;
    }

    @Override
    public SeedCall[] toSeed() {
        long minLong = (long)(min * (1L << 24));
        long maxLong = (long)(max * (1L << 24));

        long minSeed = minLong << 24;
        long maxSeed = maxLong << 24 | MASK_24;

        return new SeedCall[] { new SeedCall(minSeed, maxSeed) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        float value = JavaRandom.nextFloat(random);
        return min <= value && value <= max;
    }

    private static final long MASK_24 = (1L << 24) - 1;

    private final float min, max;
}
