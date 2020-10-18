package com.rayferric.regen.math.optimization;

import com.rayferric.regen.math.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class used to construct {@link LinearProgram} instances.
 */
public class LinearProgramBuilder {
    /**
     * Constructs a builder of universal size.
     * The expected dimension will be determined when the first constraint is added.
     */
    public LinearProgramBuilder() {
        size = -1;
        constraints = new ArrayList<>();
    }

    /**
     * Constructs a copy of a builder. Creates individual copy of every constraint.
     *
     * @param other the other builder
     */
    public LinearProgramBuilder(@NotNull LinearProgramBuilder other) {
        size = other.size;
        constraints = new ArrayList<>(other.constraints.size());

        for(LinearConstraint constraint : other.constraints)
            constraints.add(new LinearConstraint(constraint));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < constraints.size(); i++) {
            if(i != 0) builder.append("\n");
            builder.append(constraints.get(i));
        }
        return builder.toString();
    }

    /**
     * Constructs a {@link LinearProgram} from collected constraints.
     *
     * @return linear program
     *
     * @throws IllegalStateException if the constraints are not feasible
     */
    public LinearProgram build() {
        int numConstraints = constraints.size();
        int variables = size + numConstraints;
        int constraintIdx = 0, slackIdx = 0;

        // Allocate {constraints.size()} base rows, plus {size} extra rows for possible slack constraints:

        // Allocate columns for {size} factors:
        Matrix nonBasics = new Matrix(size, variables);

        // Allocate columns for {constraints.size()} base slacks,
        // plus {2 * size} many extra slack variables, and the RHS value:
        Matrix basics = new Matrix(numConstraints + 2 * size + 1, variables);

        AugmentedMatrix combinedTable = new AugmentedMatrix(nonBasics, basics);

        while(constraintIdx < numConstraints) {
            LinearConstraint constraint = constraints.get(constraintIdx);
            LinearConstraint.Type type = constraint.getType();

            // Start first {constraints.size()} rows with constraint factors:
            nonBasics.setRow(constraintIdx, constraint.getGradient());

            // Put values in the last column:
            basics.set(numConstraints + 2 * size, constraintIdx, constraint.getValue());

            // Add base slacks:
            if(type == LinearConstraint.Type.LEQUAL)
                basics.set(slackIdx++, constraintIdx, Fraction.ONE);
            else if(type == LinearConstraint.Type.GEQUAL)
                basics.set(slackIdx++, constraintIdx, Fraction.MINUS_ONE);

            constraintIdx++;
        }

        // Reduce non-basic table to remove real variables without the sign constraint:
        int[] pivotCells = GaussJordan.run(combinedTable);

        // Add slacks for non-basics that could not be removed add a constraint with 2 slacks:
        for(int col = 0; col < size; col++) {
            if(pivotCells[col] != -1)
                continue;

            nonBasics.set(col, constraintIdx, Fraction.ONE);
            basics.set(slackIdx++, constraintIdx, Fraction.ONE);
            basics.set(slackIdx++, constraintIdx, Fraction.MINUS_ONE);

            constraintIdx++;
        }

        // This time reduce the whole table:
        pivotCells = GaussJordan.run(combinedTable, true);

        // The total number of viable constraints:
        int numViableConstraints = Arrays.stream(pivotCells).max().orElseThrow(IllegalStateException::new) + 1;
        // {slackIdx} now indicates the total number of slacks.

        if(constraintIdx == size)
            throw new IllegalStateException("The constraints are not feasible.");

        // Yeet the reduced reals and use basics instead (allocate 1 extra column for the RHS):
        Matrix transform = new Matrix(slackIdx + 1, size);
        Matrix table = new Matrix(slackIdx + 1, constraintIdx - size);

        for(int row = 0; row < size; row++) {
            // Transfer the LHS:
            for(int col = 0; col < slackIdx; col++)
                transform.set(col, row, basics.get(col, row));

            // RHS:
            transform.set(slackIdx, row, basics.get(basics.getWidth() - 1, row));
        }

        for(int row = size; row < constraintIdx; row++) {
            for(int col = 0; col < slackIdx; ++col)
                table.set(col, row - size, basics.get(col, row));

            table.set(slackIdx, row - size, basics.get(basics.getWidth() - 1, row));
        }

        return new LinearProgram(transform, table);
    }

    /**
     * Adds a constraint in form Ax == value.
     *
     * @param gradient the coefficients in the new constraint
     * @param value    value of the constraint
     */
    public void addEquality(@NotNull Vector gradient, @NotNull Fraction value) {
        addConstraint(gradient, LinearConstraint.Type.EQUAL, value);
    }

    /**
     * Adds a constraint in form Ax >= value.
     *
     * @param gradient the coefficients in the new constraint
     * @param value    value of the constraint
     */
    public void addMin(@NotNull Vector gradient, @NotNull Fraction value) {
        addConstraint(gradient, LinearConstraint.Type.GEQUAL, value);
    }

    /**
     * Adds a constraint in form Ax <= value.
     *
     * @param gradient the coefficients in the new constraint
     * @param value    value of the constraint
     */
    public void addMax(@NotNull Vector gradient, @NotNull Fraction value) {
        addConstraint(gradient, LinearConstraint.Type.LEQUAL, value);
    }

    /**
     * Adds a constraint in form min <= Ax <= max.
     *
     * @param min      minimum value of the constraint
     * @param gradient the coefficients in the new constraint
     * @param max      maximum value of the constraint
     */
    public void addBounds(@NotNull Fraction min, @NotNull Vector gradient, @NotNull Fraction max) {
        addMin(gradient, min);
        addMax(gradient, max);
    }

    /**
     * Adds multiple constraints in form min <= Ax <= max.
     *
     * @param min   vector of minimum values
     * @param basis matrix with gradients as rows
     * @param max   vector of maximum values
     */
    public void addBoundedBasis(@NotNull Vector min, @NotNull Matrix basis, @NotNull Vector max) {
        int height = basis.getHeight();

        if(min.getSize() != height || max.getSize() != height)
            throw new IllegalArgumentException("The bounding vectors have to be of the same height as the basis.");

        for(int i = 0; i < height; i++)
            addBounds(min.get(i), basis.getRow(i), max.get(i));
    }

    /**
     * Returns the size of this builder, which is the expected dimension of every gradient.
     * <p>Size is determined by the size of the very first constraint collected.
     *
     * @return size
     */
    public int getSize() {
        return size;
    }

    private int size;
    private final List<LinearConstraint> constraints;

    private void addConstraint(@NotNull Vector factors, @NotNull LinearConstraint.Type type, @NotNull Fraction value) {
        if(constraints.size() == 0)
            size = factors.getSize();

        if(factors.getSize() != size)
            throw new IllegalArgumentException("All constraints must be of the same size.");

        constraints.add(new LinearConstraint(factors, type, value));
    }
}
