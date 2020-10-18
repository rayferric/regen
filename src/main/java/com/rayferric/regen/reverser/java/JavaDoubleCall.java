package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextDouble()}.
 */
public class JavaDoubleCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static int SKIPS = 2;

    /**
     * Constructs a new double call.
     *
     * @param value value returned by the random call
     */
    public JavaDoubleCall(double value) {
        this(value, value);
    }

    /**
     * Constructs a new double call.
     *
     * @param min minimum value returned by the random call
     * @param max maximum value returned by the random call
     */
    public JavaDoubleCall(double min, double max) {
        this(min, max, false, false);
    }

    /**
     * Constructs a new double call.
     *
     * @param min          minimum value returned by the random call
     * @param max          maximum value returned by the random call
     * @param minExclusive should <b>value == min</b> be disregarded
     * @param maxExclusive should <b>value == max</b> be disregarded
     */
    public JavaDoubleCall(double min, double max, boolean minExclusive, boolean maxExclusive) {
        super(SKIPS);
        this.min = minExclusive ? Math.nextUp(min) : min;
        this.max = maxExclusive ? Math.nextDown(max) : max;
    }

    @Override
    public SeedCall[] toSeed() {
        long minLong = (long)(min * (1L << 53));
        long maxLong = (long)(max * (1L << 53));

        long minSeed1 = (minLong >>> 27) << 22;
        long maxSeed1 = ((maxLong >>> 27) << 22) | MASK_22;

        // Ignore the second call if its bounds cannot be recovered:
        if(minLong >>> 27 == maxLong >>> 27) {
            long minSeed2 = (minLong & MASK_27) << 21;
            long maxSeed2 = ((maxLong & MASK_27) << 21) | MASK_21;

            return new SeedCall[] { new SeedCall(minSeed1, maxSeed1), new SeedCall(minSeed2, maxSeed2) };
        }

        return new SeedCall[] { new SeedCall(minSeed1, maxSeed1) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        double value = JavaRandom.nextDouble(random);
        return min <= value && value <= max;
    }

    private static final long MASK_21 = (1L << 21) - 1;
    private static final long MASK_22 = (1L << 22) - 1;
    private static final long MASK_27 = (1L << 27) - 1;

    private final double min, max;
}
