package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Enables execution of elementary row operations on multiple matrices simultaneously.
 */
public class AugmentedMatrix {
    /**
     * Constructs an augmented matrix.
     *
     * @param main the main matrix
     */
    public AugmentedMatrix(@NotNull Matrix main) {
        this(main, new Matrix[0]);
    }

    /**
     * Constructs an augmented matrix with secondary matrices.
     *
     * @param main   the main matrix
     * @param others secondary matrices
     */
    public AugmentedMatrix(@NotNull Matrix main, @NotNull Matrix... others) {
        int height = main.getHeight();

        for(Matrix other : others) {
            if(other.getHeight() != height)
                throw new IllegalArgumentException(
                        "All secondary matrices must be of the same height as the main one.");
        }

        this.main = main;
        this.others = others;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        AugmentedMatrix that = (AugmentedMatrix)o;
        return Objects.equals(main, that.main) &&
                Arrays.equals(others, that.others);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(main);
        result = 31 * result + Arrays.hashCode(others);
        return result;
    }

    @Override
    public String toString() {
        // Create line builders:
        StringBuilder[] lineBuilders = new StringBuilder[getHeight()];
        for(int i = 0; i < lineBuilders.length; i++)
            lineBuilders[i] = new StringBuilder();

        // Build lines:
        forAll(matrix -> {
            String[] lines = matrix.toString().split("\\r?\\n");
            for(int j = 0; j < lineBuilders.length; j++) {
                if(matrix != main) lineBuilders[j].append(" : ");
                lineBuilders[j].append(lines[j]);
            }
        });

        // Join lines into a string:
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < lineBuilders.length; i++) {
            StringBuilder lineBuilder = lineBuilders[i];
            if(i != 0) builder.append("\n");
            builder.append(lineBuilder.toString());
        }
        return builder.toString();
    }

    /**
     * Returns a column in this augmented matrix.
     * <p>Any operations issued on that vector will be reflected in the matrix.
     *
     * @param index column index
     *
     * @return column vector
     *
     * @throws IndexOutOfBoundsException when the index is out of bounds
     */
    public Vector getColumn(int index) {
        for(int i = -1; i < others.length; i++) {
            Matrix matrix = main;
            if(i != -1) matrix = others[i];

            int width = matrix.getWidth();
            if(index >= width) {
                index -= width;
                continue;
            }

            return matrix.getColumn(index);
        }

        throw new IndexOutOfBoundsException("Column index is out of bounds.");
    }

    /**
     * Returns a value from this augmented matrix.
     *
     * @param col column index
     * @param row row index
     *
     * @return cell value
     *
     * @throws IndexOutOfBoundsException if any index of the two is out of bounds
     */
    public Fraction get(int col, int row) {
        return getColumn(col).get(row);
    }

    /**
     * Sets a value in this augmented matrix.
     *
     * @param col   column index
     * @param row   row index
     * @param value value
     *
     * @throws IndexOutOfBoundsException if any index of the two is out of bounds
     */
    public void set(int col, int row, @NotNull Fraction value) {
        getColumn(col).set(row, value);
    }

    /**
     * Swaps two rows in the augmented matrix.
     *
     * @param dst destination row index
     * @param src source row index
     */
    public void swapRows(int dst, int src) {
        forAll(m -> m.swapRows(dst, src));
    }

    /**
     * Normalizes the pivot row, so that the value of the pivoted cell is equal to 1.
     * Then, subtracts multiples of that row from the other rows to zero the rest of the cells in the pivot column.
     *
     * @param col column index
     * @param row row index
     */
    public void pivotCell(int col, int row) {
        // Shall the pivot point be equal to one:
        Fraction divisor = get(col, row);
        forAll(m -> m.getRow(row).divAndSet(divisor));

        // Zero other cells in the pivot column:
        for(int i = 0; i < getHeight(); i++) {
            if(i == row) continue;
            Fraction scale = get(col, i).negate();
            addScaledRow(i, row, scale);
        }
    }

    /**
     * Returns the total width of the augmented matrix.
     *
     * @return width
     */
    public int getWidth() {
        return main.getWidth() + Arrays.stream(others).mapToInt(Matrix::getWidth).sum();
    }

    /**
     * Returns the height of this augmented matrix.
     *
     * @return height
     */
    public int getHeight() {
        return main.getHeight();
    }

    /**
     * Returns the main matrix.
     *
     * @return main
     */
    public Matrix getMain() {
        return main;
    }

    /**
     * Returns the underlying array of secondary matrices.
     *
     * @return others
     */
    public Matrix[] getOthers() {
        return others;
    }

    private final Matrix main;
    private final Matrix[] others;

    private void forAll(@NotNull Consumer<Matrix> consumer) {
        consumer.accept(main);
        for(Matrix other : others)
            consumer.accept(other);
    }

    private void addScaledRow(int dst, int src, @NotNull Fraction scale) {
        forAll(m -> {
            Vector row = m.getRow(src);
            m.getRow(dst).addAndSet(row.mul(scale));
        });
    }
}
