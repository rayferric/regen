package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextInt(int)}. Does not support non-power-of-two ranges.
 */
public class JavaIntegerRangeCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static final int SKIPS = 1;

    /**
     * Constructs a new ranged integer call.
     *
     * @param value value returned by the random call
     */
    public JavaIntegerRangeCall(int range, int value) {
        this(range, value, value);
    }

    /**
     * Constructs a new ranged integer call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public JavaIntegerRangeCall(int range, int min, int max) {
        super(SKIPS);

        if(range <= 0)
            throw new IllegalArgumentException("Range must be positive.");

        if((range & -range) != range)
            throw new IllegalArgumentException("Range must be a power of two.");

        this.range = range;
        this.min = min;
        this.max = max;
    }

    @Override
    public SeedCall[] toSeed() {
        long minSeed = (((long)min << 31) / range) << 17;
        long maxSeed = (((((long)max << 31) | MASK_31) / range) << 17) | MASK_17;

        return new SeedCall[] { new SeedCall(minSeed, maxSeed) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        int value = JavaRandom.nextInt(random, range);
        return min <= value && value <= max;
    }

    private static final long MASK_17 = (1L << 17) - 1;
    private static final long MASK_31 = (1L << 31) - 1;

    private final int range, min, max;
}
