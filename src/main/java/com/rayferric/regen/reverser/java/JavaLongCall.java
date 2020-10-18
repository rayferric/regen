package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextLong()}.
 */
public class JavaLongCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static int SKIPS = 2;

    /**
     * Constructs a new long call.
     *
     * @param value value returned by the random call
     */
    public JavaLongCall(long value) {
        this(value, value);
    }

    /**
     * Constructs a new long call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public JavaLongCall(long min, long max) {
        super(SKIPS);
        this.min = min;
        this.max = max;
    }

    @Override
    public SeedCall[] toSeed() {
        long minSeed1 = (min >>> 32) << 16;
        long maxSeed1 = ((max >>> 32) + 2) << 16 | MASK_16;

        // Ignore the second call if its bounds cannot be recovered:
        if(min >>> 32 == max >>> 32) {
            long minSeed2 = (min & MASK_32) << 16;
            long maxSeed2 = ((max & MASK_32) << 16) | MASK_16;

            return new SeedCall[] { new SeedCall(minSeed1, maxSeed1), new SeedCall(minSeed2, maxSeed2) };
        }

        return new SeedCall[] { new SeedCall(minSeed1, maxSeed1) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        long value = JavaRandom.nextLong(random);
        return min <= value && value <= max;
    }

    private static final long MASK_16 = (1L << 16) - 1;
    private static final long MASK_32 = (1L << 32) - 1;

    private final long min, max;
}
