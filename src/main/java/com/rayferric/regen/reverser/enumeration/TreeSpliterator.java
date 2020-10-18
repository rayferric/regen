package com.rayferric.regen.reverser.enumeration;

import com.rayferric.regen.math.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * An internal spliterator used to enumerate the branches of the branch-and-bound routine.
 */
public class TreeSpliterator implements Spliterator<Vector> {
    /**
     * Constructs a new spliterator with given children nodes.
     *
     * @param children the branch sub-nodes
     */
    public TreeSpliterator(@NotNull Queue<TreeNode> children) {
        this.children = children;
    }

    @Override
    public boolean tryAdvance(@NotNull Consumer<? super Vector> action) {
        while(!children.isEmpty()) {
            if(children.peek().spliterator().tryAdvance(action))
                return true;

            children.remove();
        }

        return false;
    }

    @Override
    public Spliterator<Vector> trySplit() {
        if(children.isEmpty())
            return null;

        if(children.size() == 1) {
            Spliterator<Vector> child = this.children.peek().spliterator();

            if(child != null)
                return child.trySplit();

            return null;
        }

        Queue<TreeNode> half = new LinkedList<>();

        int size = children.size() / 2;
        for(int i = 0; i < size; i++)
            half.add(children.remove());

        return new TreeSpliterator(half);
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return NONNULL | IMMUTABLE | ORDERED | DISTINCT;
    }

    @Override
    public void forEachRemaining(@NotNull Consumer<? super Vector> action) {
        while(!children.isEmpty())
            children.remove().spliterator().forEachRemaining(action);
    }

    private final Queue<TreeNode> children;
}
