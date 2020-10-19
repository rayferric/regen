package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

/**
 * Static class housing methods used in Lenstra-Lenstra-Lovasz lattice reduction algorithm.
 */
public class LLL {
    /**
     * Reduces a basis using the Lenstra-Lenstra-Lovasz algorithm.
     *
     * @param basis the basis to reduce
     * @param delta reduction quality
     *
     * @return reduced basis
     */
    public static Matrix run(@NotNull Matrix basis, @NotNull Fraction delta) {
        return new LLL(basis).reduce(delta);
    }

    private final Matrix basis, gso, mu;
    private final Vector norms;

    private LLL(@NotNull Matrix basis) {
        int width = basis.getWidth();
        int height = basis.getHeight();

        this.basis = basis;
        gso = new Matrix(width, height);
        mu = new Matrix(width, width);
        norms = Vector.zero(width);
    }

    private Matrix reduce(@NotNull Fraction delta) {
        int width = basis.getWidth();

        int k = 1, kMax = 0;
        boolean updateGSO = true;
        gso.setColumn(0, basis.getColumn(0));
        norms.set(0, basis.getColumn(0).sdot());

        while(k < width) {
            if(k > kMax && updateGSO) {
                kMax = k;
                updateGso(k);
            }
            red(k, k - 1);
            if(lovaszCondition(k, delta)) {
                for(int l = k - 2; l >= 0; l--) {
                    red(k, l);
                }
                k++;
                updateGSO = true;
            } else {
                swap(k, kMax);
                k = Math.max(1, k - 1);
                updateGSO = false;
            }
        }

        int zeroCol = 0;
        while(zeroCol < width) {
            if(!basis.getColumn(zeroCol).isZero())
                break;
            zeroCol++;
        }
        final int startingCol = zeroCol;
        return new Matrix(width - startingCol, basis.getHeight(), (x, y) -> basis.get(startingCol + x, y));
    }

    private void updateGso(int k) {
        Vector newCol = new Vector(basis.getColumn(k));
        for(int j = 0; j <= k - 1; j++) {
            if(!norms.get(j).equals(Fraction.ZERO))
                mu.set(j, k, basis.getColumn(k).dot(gso.getColumn(j)).div(norms.get(j)));
            else
                mu.set(j, k, Fraction.ZERO);
            newCol.subAndSet(gso.getColumn(j).mul(mu.get(j, k)));
        }
        gso.setColumn(k, newCol);
        norms.set(k, newCol.sdot());
    }

    private boolean lovaszCondition(int k, @NotNull Fraction delta) {
        Fraction factor = delta.sub(mu.get(k - 1, k).pow(2));
        return norms.get(k).compareTo(norms.get(k - 1).mul(factor)) >= 0;
    }

    private void red(int i, int j) {
        Fraction gs = mu.get(j, i).round();
        if(gs.equals(Fraction.ZERO))
            return;

        // When |gs| > 1/2:
        basis.getColumn(i).subAndSet(basis.getColumn(j).mul(gs));
        mu.set(j, i, mu.get(j, i).sub(gs));
        for(int col = 0; col < j; col++) {
            mu.set(col, i, mu.get(col, i).sub(mu.get(col, j).mul(gs)));
        }
    }

    private void swap(int k, int kMax) {
        basis.swapColumns(k, k - 1);
        if(k > 1) {
            for(int j = 0; j < k - 1; j++)
                mu.swapCells(j, k, j, k - 1);
        }
        Fraction tmu = mu.get(k - 1, k);
        Fraction tB = norms.get(k).add(tmu.mul(tmu).mul(norms.get(k - 1)));
        if(tB.equals(Fraction.ZERO)) {
            norms.set(k, norms.get(k - 1));
            norms.set(k - 1, Fraction.ZERO);
            gso.swapColumns(k, k - 1);
            for(int i = k + 1; i <= kMax; i++) {
                mu.set(k, i, mu.get(k - 1, i));
                mu.set(k - 1, i, Fraction.ZERO);
            }
        } else if(norms.get(k).equals(Fraction.ZERO) && !tmu.equals(Fraction.ZERO)) {
            norms.set(k - 1, tB);
            gso.getColumn(k - 1).mulAndSet(tmu);
            mu.set(k - 1, k, Fraction.ONE.div(tmu));
            for(int i = k + 1; i <= kMax; i++)
                mu.set(k - 1, i, mu.get(k - 1, i).div(tmu));
        } else {
            Fraction t = norms.get(k - 1).div(tB);
            mu.set(k - 1, k, tmu.mul(t));
            Vector b = new Vector(gso.getColumn(k - 1));
            gso.setColumn(k - 1, gso.getColumn(k).add(b.mul(tmu)));
            gso.setColumn(k, (b.mul(norms.get(k).div(tB)).sub(gso.getColumn(k).mul(mu.get(k - 1, k)))));
            norms.set(k, norms.get(k).mul(t));
            norms.set(k - 1, tB);
            for(int i = k + 1; i <= kMax; i++) {
                t = mu.get(k, i);
                mu.set(k, i, mu.get(k - 1, i).sub(tmu.mul(t)));
                mu.set(k - 1, i, t.add(mu.get(k - 1, k).mul(mu.get(k, i))));
            }
        }
    }
}
