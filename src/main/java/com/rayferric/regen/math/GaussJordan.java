package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Static class housing methods used in the Gauss-Jordan elimination algorithm.
 */
public class GaussJordan {
    /**
     * Performs the Gauss-Jordan elimination on an augmented matrix. Reduces only the main matrix.
     *
     * @param matrix augmented matrix to be processed
     *
     * @return the list of pivot cells ([col] = row), contains -1 for every unpivoted column
     */
    public static int[] run(@NotNull AugmentedMatrix matrix) {
        return run(matrix, false);
    }

    /**
     * Performs the Gauss-Jordan elimination on an augmented matrix.
     *
     * @param matrix       augmented matrix to be processed
     * @param reduceOthers whether all matrices should be reduced or just the main one
     *
     * @return the list of pivot cells ([col] = row), contains -1 for every unpivoted column
     */
    public static int[] run(@NotNull AugmentedMatrix matrix, boolean reduceOthers) {
        int width = reduceOthers ? matrix.getWidth() : matrix.getMain().getWidth();
        int height = matrix.getHeight();

        int[] pivotCells = new int[width];
        Arrays.fill(pivotCells, -1);

        int col = 0, row = 0;

        // Iterate over the columns:
        while(col < width && row < height) {
            int pivotRow = findPivotRow(matrix, col, row);

            if(pivotRow != -1) {
                matrix.swapRows(row, pivotRow);
                matrix.pivotCell(col, row);

                pivotCells[col] = row++;
            }
            col++;
        }

        return pivotCells;
    }

    /**
     * Finds the pivot row in column. Used for partial pivoting.
     *
     * @param matrix augmented solution
     * @param col    column index
     * @param row    the starting row
     *
     * @return row index
     */
    private static int findPivotRow(@NotNull AugmentedMatrix matrix, int col, int row) {
        // Iterate over the rows and pick a non-zero value from the pivot column:
        for(int i = row; i < matrix.getHeight(); i++) {
            if(matrix.get(col, i).sgn() != 0)
                return i;
        }

        return -1;
    }
}
