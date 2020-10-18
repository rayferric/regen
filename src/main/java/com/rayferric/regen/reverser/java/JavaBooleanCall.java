package com.rayferric.regen.reverser.java;

import com.rayferric.regen.reverser.Random;
import com.rayferric.regen.reverser.RandomCall;
import com.rayferric.regen.reverser.SeedCall;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a call to {@link JavaRandom#nextBoolean()}.
 *
 * <p>Implemented purely for the sake of completeness. It's just better to skip this call.
 */
public class JavaBooleanCall extends RandomCall {
    /**
     * This is how many seed updates this call requires.
     */
    public static final int SKIPS = 1;

    /**
     * Constructs a new boolean call.
     *
     * @param value value returned by the random call
     */
    public JavaBooleanCall(boolean value) {
        super(SKIPS);
        this.value = value;
    }

    @Override
    public SeedCall[] toSeed() {
        long longValue = value ? 1L : 0L;

        long minSeed = longValue << 47;
        long maxSeed = longValue << 47 | MASK_47;

        return new SeedCall[] { new SeedCall(minSeed, maxSeed) };
    }

    @Override
    public boolean validate(@NotNull Random random) {
        return value == JavaRandom.nextBoolean(random);
    }

    private static final long MASK_47 = (1L << 47) - 1;

    private final boolean value;
}
