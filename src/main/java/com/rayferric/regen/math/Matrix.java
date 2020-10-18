package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mutable matrix of arbitrary size.
 */
public class Matrix {
    /**
     * Base interface for all cell-generating lambdas.
     */
    @FunctionalInterface
    public interface Generator {
        Fraction run(int col, int row);
    }

    /**
     * Constructs a zero matrix.
     *
     * @param width  number of columns
     * @param height number of rows
     *
     * @throws IllegalArgumentException when one of the passed dimensions is negative
     */
    public Matrix(int width, int height) {
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Matrix dimension cannot be negative.");

        this.width = width;
        this.height = height;

        array = new Fraction[width * height];
        Arrays.fill(array, Fraction.ZERO);
    }

    /**
     * Constructs a matrix using a generator to create its cells.
     *
     * @param width     number of columns
     * @param height    number of rows
     * @param generator the generator lambda
     *
     * @throws IllegalArgumentException when one of the passed dimensions is negative
     */
    public Matrix(int width, int height, @NotNull Generator generator) {
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Matrix dimension cannot be negative.");

        this.width = width;
        this.height = height;

        array = new Fraction[width * height];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++)
                set(i, j, generator.run(i, j));
        }
    }

    /**
     * Constructs a matrix from a two dimensional integer array.
     * <p>The first dimension corresponds to the number of rows.
     *
     * @param array two dimensional array
     */
    public Matrix(@NotNull long[][] array) {
        height = array.length;
        width = height == 0 ? 0 : array[0].length;

        for(long[] rowArray : array) {
            if(rowArray.length != width)
                throw new IllegalArgumentException("The array must be square.");
        }

        this.array = new Fraction[width * height];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++)
                set(i, j, new Fraction(array[j][i]));
        }
    }

    /**
     * Constructs a copy of a matrix.
     *
     * @param other the other matrix
     */
    public Matrix(@NotNull Matrix other) {
        width = other.width;
        height = other.height;

        array = Arrays.copyOfRange(other.array, 0, width * height);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Matrix other = (Matrix)o;
        return width == other.width && height == other.height && Arrays.equals(array, other.array);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, Arrays.hashCode(array));
    }

    @Override
    public String toString() {
        StringBuilder[] builders = new StringBuilder[height];

        for(int i = 0; i < height; i++)
            builders[i] = new StringBuilder();

        for(int i = 0; i < width; i++) {
            int maxLength = 0;
            for(int j = 0; j < height; j++) {
                int length = getRow(j).get(i).toString().length();
                if(length > maxLength) maxLength = length;
            }
            String formatCode = String.format("%%%ds", maxLength);

            for(int j = 0; j < height; j++) {
                builders[j].append(String.format(formatCode, getRow(j).get(i).toString()));
                if(i != width - 1) builders[j].append(" | ");
            }
        }

        StringBuilder finalBuilder = new StringBuilder();
        for(int i = 0; i < height; i++) {
            if(i != height - 1) builders[i].append("\n");
            finalBuilder.append(builders[i]);
        }

        return finalBuilder.toString();
    }

    /**
     * Constructs an identity matrix.
     *
     * @param size number of both rows and columns
     *
     * @return identity matrix
     */
    public static Matrix identity(int size) {
        return new Matrix(size, size, (x, y) -> (x == y) ? Fraction.ONE : Fraction.ZERO);
    }

    /**
     * Constructs a single-column matrix.
     *
     * @param column column vector
     *
     * @return single-column matrix
     */
    public static Matrix ofColumn(@NotNull Vector column) {
        return new Matrix(1, column.getSize(), (x, y) -> column.get(y));
    }

    /**
     * Constructs a single-row matrix.
     *
     * @param row row vector
     *
     * @return single-row matrix
     */
    public static Matrix ofRow(@NotNull Vector row) {
        return new Matrix(1, row.getSize(), (x, y) -> row.get(x));
    }

    /**
     * Constructs square matrix with a custom diagonal.
     *
     * @param diagonal diagonal vector
     *
     * @return square matrix
     */
    public static Matrix ofDiagonal(@NotNull Vector diagonal) {
        int size = diagonal.getSize();
        return new Matrix(size, size, (x, y) -> (x == y) ? diagonal.get(x) : Fraction.ZERO);
    }

    /**
     * Returns the value of a certain cell in this matrix.
     *
     * @param col column index
     * @param row column index
     *
     * @return value of a single cell in matrix
     */
    public Fraction get(int col, int row) {
        return getColumn(col).get(row);
    }

    /**
     * Sets the value of a certain cell in this matrix.
     *
     * @param col   column index
     * @param row   row index
     * @param value new value
     */
    public void set(int col, int row, @NotNull Fraction value) {
        getColumn(col).set(row, value);
    }

    /**
     * Swaps two cells.
     *
     * @param firstCol  column index of the first cell
     * @param firstRow  row index of the first cell
     * @param secondCol column index of the second cell
     * @param secondRow row index of the second cell
     */
    public void swapCells(int firstCol, int firstRow, int secondCol, int secondRow) {
        Fraction tmp = get(firstCol, firstRow);

        set(firstCol, firstRow, get(secondCol, secondRow));
        set(secondCol, secondRow, tmp);
    }

    /**
     * Returns a column in the matrix as a vector with shared memory.
     * <p>Any operations issued on that vector will be reflected in the matrix.
     *
     * @param index column index
     *
     * @return vector view of a column
     */
    public Vector getColumn(int index) {
        if(index < 0 || index >= width)
            throw new IndexOutOfBoundsException("Column index must fall within range [0, width).");

        // Columns lie continuously in memory, so stride is 1.
        return Vector.viewOf(height, 1, height * index, array);
    }

    /**
     * Sets the cells in this column to values supplied by the given vector.
     *
     * @param index column index
     * @param col   the column vector
     */
    public void setColumn(int index, @NotNull Vector col) {
        if(height != col.getSize())
            throw new IllegalArgumentException(
                    "The size of the column vector must be equal the height of this matrix.");

        getColumn(index).set(col);
    }

    /**
     * Swaps two columns.
     *
     * @param first  index of the first column
     * @param second index of the second column
     */
    public void swapColumns(int first, int second) {
        // We must copy the vector to separate its memory.
        Vector tmp = new Vector(getColumn(first));

        setColumn(first, getColumn(second));
        setColumn(second, tmp);
    }

    /**
     * Returns a row in the matrix as a vector with shared memory.
     * <p>Any operations issued on that vector will be reflected in the matrix.
     *
     * @param index row index
     *
     * @return vector view of a row
     */
    public Vector getRow(int index) {
        if(index < 0 || index >= height)
            throw new IndexOutOfBoundsException("Row index must fall within range [0, height).");

        // Rows, unlike columns, need a stride.
        return Vector.viewOf(width, height, index, array);
    }

    /**
     * Sets the cells in this row to values supplied by the given vector.
     *
     * @param index row index
     * @param row   the row vector
     */
    public void setRow(int index, @NotNull Vector row) {
        if(width != row.getSize())
            throw new IllegalArgumentException("The size of the row vector must be equal the width of this matrix.");

        getRow(index).set(row);
    }

    /**
     * Swaps two rows.
     *
     * @param first  index of the first row
     * @param second index of the second row
     */
    public void swapRows(int first, int second) {
        // We must copy the vector to separate its memory.
        Vector tmp = new Vector(getRow(first));

        setRow(first, getRow(second));
        setRow(second, tmp);
    }

    /**
     * Returns the diagonal of this matrix as a vector with shared memory.
     * <p>Any operations issued on that vector will be reflected in the matrix.
     *
     * @return vector view of the diagonal
     */
    public Vector getDiagonal() {
        return Vector.viewOf(width, height + 1, 0, array);
    }

    /**
     * Sets the cells in the diagonal to values supplied by the given vector.
     *
     * @param diagonal the diagonal vector
     */
    public void setDiagonal(@NotNull Vector diagonal) {
        if(width != diagonal.getSize())
            throw new IllegalArgumentException(
                    "The size of the diagonal vector must be equal the width of this matrix.");

        getDiagonal().set(diagonal);
    }

    /**
     * Computes the determinant of this matrix (recursive routine).
     *
     * @return |this|
     */
    public Fraction getDeterminant() {
        if(!isSquare())
            throw new IllegalStateException("The matrix must be square.");

        if(width == 0) return Fraction.ZERO;
        if(width == 1) return get(0, 0);

        Fraction det = Fraction.ZERO;

        // Iterate every column:
        for(int i = 0; i < width; i++) {
            int excludedCol = i;
            Matrix subMatrix = new Matrix(width - 1, height - 1, (x, y) -> {
                int originalCol = x < excludedCol ? x : x + 1;
                int originalRow = y + 1;
                return get(originalCol, originalRow);
            });

            Fraction factor = get(i, 0);
            Fraction subDet = subMatrix.getDeterminant();
            subDet = subDet.mul(factor);

            det = det.add(i % 2 == 0 ? subDet : subDet.negate());
        }

        return det;
    }

    /**
     * Multiplies this matrix by an another one without altering their state.
     *
     * @param other the other matrix
     *
     * @return this * other
     */
    public Matrix mul(@NotNull Matrix other) {
        if(width != other.height)
            throw new IllegalArgumentException(
                    "The number of columns in the first matrix must be equal to the number of rows in another.");

        return new Matrix(other.width, height, (x, y) -> getRow(y).dot(other.getColumn(x)));
    }

    /**
     * Multiplies this matrix by a vector.
     *
     * @param vector the vector of the right size
     *
     * @return this * vector
     */
    public Vector mul(@NotNull Vector vector) {
        if(width != vector.getSize())
            throw new IllegalArgumentException("Size of the vector must be equal the width of this matrix.");

        return new Vector(height, index -> getRow(index).dot(vector));
    }

    /**
     * Returns the transpose of this matrix.
     *
     * @return this<sup>T</sup>
     */
    public Matrix transpose() {
        return new Matrix(height, width, (x, y) -> get(y, x));
    }

    /**
     * Returns the inverse of this matrix. Produces partially reduced table for singular matrices.
     *
     * @return this<sup>-1</sup>
     */
    public Matrix inverse() {
        AugmentedMatrix matrix = new AugmentedMatrix(new Matrix(this), Matrix.identity(height));
        GaussJordan.run(matrix);
        return matrix.getOthers()[0];
    }

    /**
     * Tells whether the matrix is square.
     *
     * @return true if square
     */
    public boolean isSquare() {
        return width == height;
    }

    /**
     * Returns the number of columns in this matrix.
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in this matrix.
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the underlying array of this matrix.
     *
     * @return one-dimensional array
     */
    public Fraction[] getArray() {
        return array;
    }

    private final int width, height;
    private final Fraction[] array;
}