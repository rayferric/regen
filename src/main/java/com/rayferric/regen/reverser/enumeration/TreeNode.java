package com.rayferric.regen.reverser.enumeration;

import com.rayferric.regen.math.Fraction;
import com.rayferric.regen.math.Matrix;
import com.rayferric.regen.math.Vector;
import com.rayferric.regen.math.optimization.LinearProgram;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Spliterator;

/**
 * Represents an immutable branch node in the branch-and-bound tree.
 * The objects passed to the constructor won't be copied, so they should not be modified after construction.
 */
public class TreeNode {
    /**
     * Constructs the root node of the branch-and-bound tree.
     *
     * @param basis   sorted inverse of basis bounded by the initial program
     * @param program initial linear program
     */
    public TreeNode(@NotNull Matrix basis, @NotNull LinearProgram program) {
        depth = 0;

        this.basis = basis;
        this.program = program;

        // The basis is an inverse, which means it must be square, so it doesn't matter which dimension we use:
        vertex = new Vector(basis.getWidth());
    }

    /**
     * Returns the spliterator for this node.
     * On the first call to this method, the object
     * computes this node and generates several other sub-nodes.
     *
     * @return the spliterator
     */
    public Spliterator<Vector> spliterator() {
        if(spliterator == null)
            spliterator = split();

        return spliterator;
    }

    private Spliterator<Vector> spliterator = null;
    private final int depth;
    private final Matrix basis;
    private final LinearProgram program;
    private final Vector vertex;

    private TreeNode(int depth, @NotNull Matrix basis, @NotNull LinearProgram program, @NotNull Vector vertex) {
        this.depth = depth;
        this.basis = basis;
        this.program = program;
        this.vertex = vertex;
    }

    private Spliterator<Vector> split() {
        // The basis is a square:
        int size = basis.getWidth();

        // We have reached the end of this branch:
        if(depth == size)
            return Arrays.spliterator(new Vector[] { vertex });

        Vector gradient = basis.getRow(depth);

        BigInteger min = program.minimize(gradient).dot(gradient).ceil().getNumerator();
        BigInteger max = program.maximize(gradient).dot(gradient).floor().getNumerator();

        Queue<TreeNode> children = new LinkedList<>();

        for(BigInteger i = min; i.compareTo(max) <= 0; i = i.add(BigInteger.ONE)) {
            Fraction value = new Fraction(i);

            LinearProgram newProgram = program.withEquality(gradient, value);
            Vector newVertex = vertex.add(Vector.basis(size, depth, value));

            children.add(new TreeNode(depth + 1, basis, newProgram, newVertex));
        }

        return new TreeSpliterator(children);
    }
}
