package com.rayferric.regen.reverser;

import org.jetbrains.annotations.NotNull;

/**
 * A base type for all random calls.
 */
public abstract class RandomCall {
    /**
     * Tells how many seed updates this call requires.
     * You can also use the static field {@link SeedCall#SKIPS} of the known sub-type.
     *
     * @return amount of required seed updates
     */
    public int getSkips() {
        return skips;
    }

    /**
     * Splits this call into multiple {@link SeedCall seed calls}.
     * Only used internally by the reverser.
     *
     * @return array of {@link SeedCall seed calls}
     */
    public abstract SeedCall[] toSeed();

    /**
     * Checks if the next value of that RNG satisfy the constraints for this call.
     *
     * @param generator the RNG
     *
     * @return true if satisfies
     */
    public abstract boolean validate(@NotNull Random generator);

    /**
     * Initializes the super-type.
     *
     * @param skips the amount of seed updates for this new type call
     */
    protected RandomCall(int skips) {
        this.skips = skips;
    }

    private final int skips;
}
