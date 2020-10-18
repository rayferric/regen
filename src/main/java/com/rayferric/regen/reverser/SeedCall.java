package com.rayferric.regen.reverser;

import org.jetbrains.annotations.NotNull;

/**
 * A basic {@link RandomCall} implementation. Represents a call to {@link Random#nextSeed()}.
 */
public class SeedCall extends RandomCall {
    public static final int SKIPS = 1;

    /**
     * Constructs a new seed call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public SeedCall(long min, long max) {
        super(SKIPS);
        this.min = min;
        this.max = max;
    }

    @Override
    public SeedCall[] toSeed() {
        return new SeedCall[] { this };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        long value = random.nextSeed();
        return min <= value && value <= max;
    }

    /**
     * Returns the lower bound for this seed call.
     *
     * @return minimum seed
     */
    public long minSeed() {
        return min;
    }

    /**
     * Returns the upper bound for this seed call.
     *
     * @return maximum seed
     */
    public long maxSeed() {
        return max;
    }

    private final long min, max;
}
