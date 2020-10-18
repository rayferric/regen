package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextInt()}.
 */
public class JavaIntegerCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static final int SKIPS = 1;

    /**
     * Constructs a new integer call.
     *
     * @param value value returned by the random call
     */
    public JavaIntegerCall(int value) {
        this(value, value);
    }

    /**
     * Constructs a new integer call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public JavaIntegerCall(int min, int max) {
        super(SKIPS);
        this.min = min;
        this.max = max;
    }

    @Override
    public SeedCall[] toSeed() {
        // Max seed is pad with ones instead:
        long minSeed = (long)min << 16;
        long maxSeed = ((long)max << 16) | MASK_16;

        return new SeedCall[] { new SeedCall(minSeed, maxSeed) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        int value = JavaRandom.nextInt(random);
        return min <= value && value <= max;
    }

    private static final long MASK_16 = (1L << 16) - 1;

    private final int min, max;
}
