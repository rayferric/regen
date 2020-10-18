package com.rayferric.regen.math.optimization;

import com.rayferric.regen.math.Fraction;
import com.rayferric.regen.math.Matrix;
import com.rayferric.regen.math.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a linear program. New instances can be created by using the{@link LinearProgramBuilder}
 * or by {@link #withEquality(Vector, Fraction) attaching equalities} to existing programs.
 */
public class LinearProgram {
    /**
     * Maximizes for a set of objective function factors.
     *
     * @param gradient coefficients in the objective function
     *
     * @return the solution coordinates (vertex • gradient gives optimal solution)
     *
     * @throws IllegalArgumentException if the number of factors is different than the size of the program
     * @throws IllegalStateException    if the constraints are not feasible
     */
    public Vector maximize(@NotNull Vector gradient) {
        return new LinearProgram(this).maximizeInternal(gradient);
    }

    /**
     * Minimizes for a set of objective function factors.
     *
     * @param gradient coefficients in the objective function
     *
     * @return the solution coordinates (vertex • gradient gives optimal solution)
     *
     * @throws IllegalArgumentException if the number of factors is different than the size of the program
     * @throws IllegalStateException    if the constraints are not feasible
     */
    public Vector minimize(@NotNull Vector gradient) {
        return new LinearProgram(this).maximizeInternal(gradient.negate());
    }

    /**
     * Constructs a new program with an additional equality constraint.
     *
     * @param gradient coefficients of the constraint
     * @param value    the value
     *
     * @return a separate program with this additional constraint
     */
    public LinearProgram withEquality(@NotNull Vector gradient, @NotNull Fraction value) {
        int width = table.getWidth();
        int height = table.getHeight();

        Matrix newTable = new Matrix(width, height + 1);

        for(int row = 0; row < height - 1; row++)
            newTable.setRow(row, table.getRow(row));

        newTable.setRow(height - 1, transformConstraint(gradient, value));

        if(newTable.get(width - 1, height - 1).sgn() < 0)
            newTable.getRow(height - 1).mulAndSet(Fraction.MINUS_ONE);

        int[] newBasics = Arrays.copyOf(basics, height);
        int[] newNonBasics = Arrays.copyOf(nonBasics, width - 1);

        newBasics[height - 1] = (width - 1) + (height - 1);

        return new LinearProgram(transform, newTable, newBasics, newNonBasics, 1);
    }

    /**
     * Returns the number of variables in this program,
     * which is also the expected size of gradients submitted.
     *
     * @return size
     */
    public int getSize() {
        return transform.getHeight();
    }

    /**
     * Constructs a linear program from pre-assembled tables.
     * This is an internal method used by the {@link LinearProgramBuilder}.
     *
     * @param transform the transform table
     * @param table     the main table
     */
    protected LinearProgram(@NotNull Matrix transform, @NotNull Matrix table) {
        int constraints = table.getHeight();
        int variables = table.getWidth() - 1;

        // [row] = variable
        int[] basics = new int[constraints];
        Arrays.fill(basics, -1);

        // [final col] = variable
        List<Integer> nonBasicList = new ArrayList<>();

        // Flip constraints with negative values:
        for(int row = 0; row < constraints; row++) {
            if(table.get(variables, row).sgn() < 0)
                table.getRow(row).mulAndSet(Fraction.MINUS_ONE);
        }

        for(int col = 0; col < variables; col++) {
            int count = 0;
            int index = -1;

            for(int row = 0; row < table.getHeight(); row++) {
                if(table.get(col, row).sgn() == 0)
                    continue;

                count++;
                index = row;
            }

            if(count == 1 && basics[index] == -1 && table.get(col, index).sgn() > 0) {
                table.getRow(index).divAndSet(table.get(col, index));
                basics[index] = col;
            } else
                nonBasicList.add(col);
        }

        int artificials = 0;

        for(int row = 0; row < constraints; row++) {
            if(basics[row] != -1)
                continue;

            basics[row] = variables + (artificials++);
        }

        int[] nonBasics = nonBasicList.stream().mapToInt(i -> i).toArray();
        int nonBasicCount = variables - constraints + artificials;
        Matrix newTable = new Matrix(nonBasicCount + 1, constraints + 1);

        for(int row = 0; row < constraints; row++) {
            for(int basicRow = 0; basicRow < constraints; basicRow++) {
                if(row == basicRow || basics[basicRow] >= variables)
                    continue;

                Vector rowVector = table.getRow(row);
                Vector basicVector = table.getRow(basicRow);

                rowVector.subAndSet(basicVector.mul(rowVector.get(basics[basicRow])));
            }

            for(int col = 0; col < nonBasicCount; col++)
                newTable.set(col, row, table.get(nonBasics[col], row));

            newTable.set(nonBasicCount, row, table.get(variables, row));
        }

        init(transform, newTable, basics, nonBasics, artificials);
    }

    private Matrix transform, table;
    private int[] basics, nonBasics;

    private LinearProgram(@NotNull LinearProgram other) {
        transform = other.transform;
        table = new Matrix(other.table);
        basics = Arrays.copyOf(other.basics, other.basics.length);
        nonBasics = Arrays.copyOf(other.nonBasics, other.nonBasics.length);
    }

    private LinearProgram(@NotNull Matrix transform, @NotNull Matrix table, int[] basics, int[] nonBasics,
                          int numArtificials) {
        init(transform, table, basics, nonBasics, numArtificials);
    }

    private void init(@NotNull Matrix transform, @NotNull Matrix table, int[] basics, int[] nonBasics,
                      int numArtificials) {
        int width = table.getWidth();
        int height = table.getHeight();

        int numReals = (height - 1) + (width - 1) - numArtificials;

        for(int row = 0; row < height - 1; row++) {
            if(basics[row] < numReals)
                continue;

            table.getRow(height - 1).addAndSet(table.getRow(row));
        }

        this.transform = null;
        this.table = table;
        this.basics = basics;
        this.nonBasics = nonBasics;

        solve();

        if(table.get(width - 1, height - 1).sgn() != 0)
            throw new IllegalStateException("Constraints are not feasible.");

        for(int row = 0; row < height - 1; row++) {
            if(basics[row] < numReals)
                continue;

            for(int col = 0; col < width - 1; col++) {
                if(nonBasics[col] >= numReals || table.get(col, row).sgn() == 0)
                    continue;

                pivot(col, row);
                break;
            }
        }

        this.transform = transform;
        this.table = new Matrix(width - numArtificials, height);

        for(int i = 0, j = 0; i < this.table.getWidth() - 1; i++, j++) {
            while(true) {
                if(nonBasics[j] >= numReals) {
                    j++;
                    continue;
                }

                for(int row = 0; row < height - 1; row++)
                    this.table.set(i, row, table.get(j, row));

                nonBasics[i] = nonBasics[j];
                break;
            }
        }

        for(int row = 0; row < height - 1; row++)
            this.table.set(this.table.getWidth() - 1, row, table.get(width - 1, row));
    }

    private Vector maximizeInternal(@NotNull Vector gradient) {
        if(gradient.getSize() != getSize())
            throw new IllegalArgumentException("Gradient must be of the same size as the program.");

        // Put transformed gradient in the last row:
        table.setRow(table.getHeight() - 1, transformConstraint(gradient, Fraction.ZERO));

        solve();

        // Read result offset from the last column of the transform:
        Vector vertex = transform.getColumn(transform.getWidth() - 1);
        vertex = new Vector(vertex);

        for(int row = 0; row < table.getHeight() - 1; row++) {
            Vector basicColumn = transform.getColumn(basics[row]);
            Fraction scale = table.get(table.getWidth() - 1, row);
            vertex.subAndSet(basicColumn.mul(scale));
        }

        // vertex • gradient is the maximization result, it can
        // also be obtained from the lower-right cell of {table}.

        return vertex;
    }

    private void solve() {
        int width = table.getWidth();
        int height = table.getHeight();

        while(true) {
            boolean pickAnyDelta = false;

            int entering = -1;
            int exiting = -1;

            for(int row = 0; row < height - 1; row++) {
                if(table.get(width - 1, row).sgn() == 0) {
                    pickAnyDelta = true;
                    break;
                }
            }

            // Pick the column with the largest positive delta as the entering (pivot) column:
            Fraction largestPositiveDelta = Fraction.ZERO;
            for(int col = 0; col < width - 1; col++) {
                Fraction delta = table.get(col, height - 1);

                if(delta.sgn() <= 0 || (entering != -1 && delta.compareTo(largestPositiveDelta) <= 0))
                    continue;

                largestPositiveDelta = delta;
                entering = col;

                if(pickAnyDelta)
                    break;
            }

            // If there are no pivots, we are done:
            if(entering == -1)
                break;

            // Pick the row with the smallest ratio as the exiting (pivot) row:
            Fraction smallestPositiveRatio = Fraction.ZERO;
            for(int row = 0; row < height - 1; row++) {
                Fraction value = table.get(entering, row);

                if(value.sgn() <= 0)
                    continue;

                Fraction ratio = table.get(width - 1, row).div(value);

                if(exiting != -1 && ratio.compareTo(smallestPositiveRatio) >= 0)
                    continue;

                smallestPositiveRatio = ratio;
                exiting = row;
            }

            pivot(entering, exiting);
        }
    }

    private void pivot(int entering, int exiting) {
        Fraction pivotValue = table.get(entering, exiting);
        Vector pivotRow = table.getRow(exiting);

        // Divide the pivot row by the pivot value (like in Gaussian), but invert the pivot instead:
        pivotRow.divAndSet(pivotValue);
        table.set(entering, exiting, pivotValue.inverse());

        // Subtract rows like you would do in Gaussian elimination,
        // but, in the pivoting column, put negative values divided by the pivot instead:
        for(int row = 0; row < table.getHeight(); row++) {
            if(row == exiting)
                continue;

            Fraction scale = table.get(entering, row);

            table.getRow(row).subAndSet(pivotRow.mul(scale));
            table.set(entering, row, scale.div(pivotValue).negate());
        }

        // Swap the entering and exiting variables:
        int tmp = basics[exiting];
        basics[exiting] = nonBasics[entering];
        nonBasics[entering] = tmp;
    }

    private Vector transformConstraint(@NotNull Vector gradient, @NotNull Fraction value) {
        Vector transformed = new Vector(transform.getWidth());
        Vector eliminated = new Vector(table.getWidth());

        transformed.set(transform.getWidth() - 1, value);

        for(int row = 0; row < getSize(); row++) {
            Fraction scale = gradient.get(row);
            transformed.subAndSet(transform.getRow(row).mul(scale));
        }

        for(int col = 0; col < eliminated.getSize() - 1; col++)
            eliminated.set(col, transformed.get(nonBasics[col]));

        eliminated.set(eliminated.getSize() - 1, transformed.get(transform.getWidth() - 1));

        for(int row = 0; row < table.getHeight() - 1; row++) {
            Fraction scale = transformed.get(basics[row]);
            eliminated.subAndSet(table.getRow(row).mul(scale));
        }

        return eliminated;
    }
}