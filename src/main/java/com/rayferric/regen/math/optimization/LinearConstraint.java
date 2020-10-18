package com.rayferric.regen.math.optimization;

import com.rayferric.regen.math.Fraction;
import com.rayferric.regen.math.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Mutable container for a linear constraint.
 */
public class LinearConstraint {
    /**
     * Constraint type.
     * <ul>
     *     <li>LEQUAL: factors • solution <= scalar</li>
     *     <li>EQUAL: factors • solution == scalar</li>
     *     <li>GEQUAL: factors • solution >= scalar</li>
     * </ul>
     */
    public enum Type {
        LEQUAL, EQUAL, GEQUAL
    }

    /**
     * Constructs a linear constraint.
     *
     * @param gradient a vector of left-side factors
     * @param type     the type of this constraint (<=, ==, >=)
     * @param value    right-side value
     */
    LinearConstraint(@NotNull Vector gradient, @NotNull Type type, @NotNull Fraction value) {
        this.gradient = gradient;
        this.type = type;
        this.value = value;
    }

    /**
     * Constructs a copy of a linear constraint.
     *
     * @param other the other constraint
     */
    public LinearConstraint(@NotNull LinearConstraint other) {
        gradient = other.gradient;
        type = other.type;
        value = other.value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int variable = 0;
        for(int i = 0; i < gradient.getSize(); i++) {
            Fraction factor = gradient.get(i);
            if(factor.sgn() == 0) continue;

            if(variable++ != 0) {
                if(factor.sgn() == 1)
                    builder.append(" + ");
                else if(factor.sgn() == -1)
                    builder.append(" - ");
            } else if(factor.sgn() == -1)
                builder.append("-");

            factor = factor.abs();

            if(!factor.isInteger()) builder.append('(');
            if(!factor.equals(Fraction.ONE)) builder.append(factor);
            if(!factor.isInteger()) builder.append(')');

            builder.append((char)('a' + i));
        }

        if(builder.length() == 0) builder.append(0);

        if(type == Type.LEQUAL)
            builder.append(" <= ");
        else if(type == Type.GEQUAL)
            builder.append(" >= ");
        else
            builder.append(" == ");

        return builder.append(value).toString();
    }

    /**
     * Substitutes variable for a constant.
     *
     * @param index    index of the variable
     * @param constant a value for this variable to be set to
     *
     * @return a linear constraint with one less factor
     *
     * @throws IllegalArgumentException when the index is out of bounds
     */
    public LinearConstraint substituteVariable(int index, @NotNull Fraction constant) {
        if(index < 0 || index >= gradient.getSize())
            throw new IllegalArgumentException("Variable index is out of bounds.");

        Vector newFactors = new Vector(gradient.getSize() - 1, (i) -> {
            if(i < index)
                return gradient.get(i);
            else
                return gradient.get(i + 1);
        });
        Fraction newValue = value.sub(gradient.get(index).mul(constant));

        return new LinearConstraint(newFactors, type, newValue);
    }

    /**
     * Returns the left-side factors of this constraint.
     *
     * @return factors
     */
    public Vector getGradient() {
        return gradient;
    }

    /**
     * Returns the factors type of this constraint.
     *
     * @return type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the right-side value of this constraint.
     *
     * @return value
     */
    public Fraction getValue() {
        return value;
    }

    private final Vector gradient;
    private final Type type;
    private final Fraction value;
}
