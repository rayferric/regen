package com.rayferric.regen.reverser;

import com.rayferric.regen.math.*;
import com.rayferric.regen.math.Vector;
import com.rayferric.regen.math.optimization.LinearProgram;
import com.rayferric.regen.math.optimization.LinearProgramBuilder;
import com.rayferric.regen.reverser.enumeration.TreeNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.stream.*;

/**
 * Reverse engineers possible states of a linear congruential generator (LCG)
 * based RNG, given the sequence of known constraints.
 */
public class RandomReverser {
    /**
     * Adds a random call to this builder. (See also: {@link #addFilter(RandomCall)}.)
     *
     * @param call a random call
     *
     * @return this
     */
    public RandomReverser addCall(@NotNull RandomCall call) {
        return addEntry(call, false);
    }

    /**
     * Adds a filter-only random call to this builder.
     *
     * <p>Filter-only calls do not participate in the computation
     * process and are only used to eliminate false-positives.
     *
     * @param call a random call
     *
     * @return this
     */
    public RandomReverser addFilter(@NotNull RandomCall call) {
        return addEntry(call, true);
    }

    /**
     * Skips a specified amount of seed updates.
     *
     * <p>Different types of calls may issue multiple seed updates.
     *
     * <p>Use {@link SeedCall#SKIPS} static field to look up the required amount of seed updates for any type of call.
     *
     * @param step the amount of seed updates to skip
     *
     * @return this
     */
    public RandomReverser skip(int step) {
        indexProvider += step;
        return this;
    }

    /**
     * Skips a single seed update. (See {@link #skip(int)}.
     *
     * @return this
     */
    public RandomReverser skip() {
        return skip(1);
    }

    /**
     * Finds all working seeds the RNG might have been initially set to.
     *
     * @param lcg the LCG to reverse for (e.g. {@link LCG#JAVA})
     *
     * @return a parallel {@link LongStream} that reveals solutions
     * when looped through (see: {@link LongStream#forEach(LongConsumer)})
     */
    public LongStream solve(@NotNull LCG lcg) {
        // Split and collect calls:

        List<CallEntry> seedCalls = new ArrayList<>(calls.size());

        for(CallEntry entry : calls) {
            if(!entry.filterOnly)
                seedCalls.addAll(Arrays.asList(entry.toSeed()));
        }

        int numCalls = seedCalls.size();

        // Initialize basis and other vectors:

        Matrix basis = new Matrix(numCalls + 1, numCalls);
        Vector offset = Vector.zero(numCalls);
        Vector min = Vector.zero(numCalls);
        Vector max = Vector.zero(numCalls);

        Fraction multiplier = new Fraction(lcg.getMultiplier());
        Fraction modulus = new Fraction(lcg.getModulus());

        Random random = new Random(lcg, 0);

        for(int i = 0; i < numCalls; i++) {
            CallEntry entry = seedCalls.get(i);
            SeedCall seedCall = (SeedCall)entry.call;

            if(i != 0)
                random.skip(entry.index - seedCalls.get(i - 1).index);

            basis.set(0, i, multiplier.pow(entry.index).mod(modulus));
            basis.set(i + 1, i, modulus);

            offset.set(i, new Fraction(random.getSeed()));

            min.set(i, new Fraction(seedCall.minSeed()));
            max.set(i, new Fraction(seedCall.maxSeed()));
        }

        min.subAndSet(offset);
        max.subAndSet(offset);

        // Reduce the basis:

        Vector sideLengths = new Vector(numCalls, i -> max.get(i).sub(min.get(i)).add(Fraction.ONE));

        BigInteger lcm = BigInteger.ONE;
        for(int i = 0; i < numCalls; i++) {
            BigInteger length = sideLengths.get(i).getNumerator();
            lcm = length.multiply(lcm).divide(lcm.gcd(length));
        }

        Matrix scaling = Matrix.ofDiagonal(Vector.repeat(numCalls, new Fraction(lcm)).div(sideLengths));
        basis = scaling.mul(basis);
        basis = LLL.run(basis, new Fraction(99, 100));
        basis = scaling.inverse().mul(basis);

        // Prepare enumeration:

        Matrix basisInverse = basis.inverse();

        LinearProgramBuilder builder = new LinearProgramBuilder();
        builder.addBoundedBasis(min, Matrix.identity(numCalls), max);
        LinearProgram program = builder.build();

        Fraction[] widths = new Fraction[basisInverse.getHeight()];
        List<Integer> order = new ArrayList<>(basisInverse.getHeight());

        for(int i = 0; i < basisInverse.getHeight(); i++) {
            Vector gradient = basisInverse.getRow(i);
            Fraction minValue = program.minimize(gradient).dot(gradient);
            Fraction maxValue = program.maximize(gradient).dot(gradient);

            widths[i] = maxValue.sub(minValue);
            order.add(i);
        }

        order.sort(Comparator.comparing(i -> widths[i]));

        Matrix sortedBasisInverse = new Matrix(basisInverse.getWidth(), basis.getHeight(), (x, y) -> basisInverse.get(x, order.get(y)));

        // Execute branch-and-bound:

        TreeNode root = new TreeNode(sortedBasisInverse, program);
        Stream<Vector> vertices = StreamSupport.stream(root.spliterator(), true);
        Stream<Vector> sorted = vertices.map(vertex -> new Vector(vertex.getSize(), i -> vertex.get(order.indexOf(i))));
        Stream<Vector> transformed = sorted.map(basis::mul).map(offset::add);
        LongStream seeds = transformed.mapToLong(vertex -> vertex.get(0).getNumerator().longValue());

        LCG toStart = lcg.ofStep(-(calls.get(0).index + 1));
        Random validator = new Random(lcg, 0);

        // Eliminate possible false-positives:
        return seeds.filter(seed -> {
            validator.setSeed(toStart.next(seed));
            for(int i = 0; i < calls.size(); i++) {
                CallEntry entry = calls.get(i);

                int index = entry.index;
                if(i != 0) {
                    CallEntry prevEntry = calls.get(i - 1);
                    index -= (prevEntry.index + prevEntry.call.getSkips());
                }
                validator.skip(index);

                if(!entry.call.validate(validator))
                    return false;
            }
            return true;
        }).map(toStart::next);
    }

    /**
     * Solves for the Java LCG. (see {@link #solve(LCG)}.
     *
     * @return a parallel {@link LongStream} that reveals solutions
     * when looped through (see: {@link LongStream#forEach(LongConsumer)})
     */
    public LongStream solve() {
        return solve(LCG.JAVA);
    }

    private static class CallEntry {
        public CallEntry(int index, @NotNull RandomCall call, boolean filterOnly) {
            this.index = index;
            this.call = call;
            this.filterOnly = filterOnly;
        }

        public CallEntry[] toSeed() {
            SeedCall[] seedCalls = call.toSeed();
            CallEntry[] seedEntries = new CallEntry[seedCalls.length];

            for(int i = 0; i < seedCalls.length; i++) {
                SeedCall seedCall = seedCalls[i];
                seedEntries[i] = new CallEntry(index + i, seedCall, filterOnly);
            }

            return seedEntries;
        }

        public int index;
        public RandomCall call;
        public boolean filterOnly;
    }

    private final List<CallEntry> calls = new ArrayList<>();
    private int indexProvider = 0;

    private RandomReverser addEntry(@NotNull RandomCall call, boolean filterOnly) {
        calls.add(new CallEntry(indexProvider, call, filterOnly));
        indexProvider += call.getSkips();
        return this;
    }
}
