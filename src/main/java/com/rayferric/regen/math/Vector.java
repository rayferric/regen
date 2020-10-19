package com.rayferric.regen.math;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mutable vector of arbitrary size.
 */
public class Vector {
    /**
     * Base interface for all component-generating lambdas.
     */
    @FunctionalInterface
    public interface Generator {
        Fraction run(int index);
    }

    /**
     * Base interface for all component-wise lambda operations executed on this vector.
     */
    @FunctionalInterface
    public interface Operator {
        Fraction run(Fraction lhs, Fraction rhs);
    }

    /**
     * Constructs vector wrapper for an array.
     *
     * @param array one-dimensional array
     */
    public Vector(@NotNull Fraction... array) {
        size = array.length;
        stride = 1;
        offset = 0;

        this.array = array;
    }

    /**
     * Constructs vector from an array of longs.
     *
     * @param array one-dimensional array
     */
    public Vector(@NotNull long... array) {
        this(array.length, i -> new Fraction(array[i]));
    }

    /**
     * Constructs a vector using a generator to create its components.
     *
     * @param size      number of components
     * @param generator the generator lambda
     */
    public Vector(int size, @NotNull Generator generator) {
        this(new Fraction[size]);

        for(int i = 0; i < size; i++)
            array[i] = generator.run(i);
    }

    /**
     * Constructs a copy of a vector.
     *
     * @param other the other vector
     */
    public Vector(@NotNull Vector other) {
        size = other.size;
        stride = 1;
        offset = 0;

        if(other.stride == 1)
            array = Arrays.copyOfRange(other.array, other.offset, other.offset + other.size);
        else {
            array = new Fraction[other.size];
            for(int i = 0; i < array.length; i++)
                array[i] = other.get(i);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Vector other = (Vector)o;
        return size == other.size && stride == other.stride && offset == other.offset &&
                Arrays.equals(array, other.array);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, stride, offset, Arrays.hashCode(array));
    }

    @Override
    public String toString() {
        if(size == 0) return "()";

        StringBuilder builder = new StringBuilder("(").append(get(0));

        for(int i = 1; i < size; i++)
            builder.append(", ").append(get(i));

        return builder.append(")").toString();
    }

    /**
     * Constructs a vector view of a section of array.
     *
     * @param size   number of components in the vector
     * @param stride every how many elements a vector component is located (a in ax +b)
     * @param offset memory offset of every component (b in ax + b)
     * @param array  the source array to be accessed
     *
     * @return a vector with non-continuous memory alignment
     */
    public static Vector view(int size, int stride, int offset, @NotNull Fraction[] array) {
        return new Vector(size, stride, offset, array);
    }

    /**
     * Constructs a zero vector of given size.
     *
     * @param size number of components
     */
    public static Vector zero(int size) {
        Fraction[] array = new Fraction[size];
        Arrays.fill(array, Fraction.ZERO);

        return new Vector(array);
    }

    /**
     * Constructs a vector filled with one specific value.
     *
     * @param size  number of components
     * @param value the fill value
     *
     * @return a vector filled with value
     */
    public static Vector repeat(int size, @NotNull Fraction value) {
        Fraction[] array = new Fraction[size];
        Arrays.fill(array, value);
        return new Vector(array);
    }

    /**
     * Constructs a basis vector of length 1.
     *
     * @param size  number of components in the vector
     * @param index which component should be the direction of the basis
     *
     * @return a vector with only a single component having a value
     */
    public static Vector basis(int size, int index) {
        return basis(size, index, Fraction.ONE);
    }

    /**
     * Constructs a vector with a single value being one of it's components.
     *
     * @param size  number of components in the vector
     * @param index which component should be the direction of the basis
     * @param value the scale of the basis
     *
     * @return a vector with only a single component having a value
     */
    public static Vector basis(int size, int index, @NotNull Fraction value) {
        Vector vector = Vector.zero(size);
        vector.set(index, value);
        return vector;
    }

    /**
     * Returns the value of a certain component in this vector.
     *
     * @param index component index
     *
     * @return value of a single component in vector
     *
     * @throws IndexOutOfBoundsException when the index is out of bounds
     */
    public Fraction get(int index) {
        if(index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index must fall within range [0, size).");

        return array[index * stride + offset];
    }

    /**
     * Sets the value of a certain component in this vector.
     *
     * @param index component index
     * @param value new value
     */
    public void set(int index, @NotNull Fraction value) {
        if(index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index must fall within range [0, size).");

        array[index * stride + offset] = value;
    }

    /**
     * Sets the components of this vector to the values supplied by an another one.
     *
     * @param other the other vector
     */
    public void set(@NotNull Vector other) {
        if(size != other.size)
            throw new IllegalArgumentException("Both vectors must be equal in size.");

        for(int i = 0; i < size; i++)
            set(i, other.get(i));
    }

    /**
     * Applies a component-wise {@link Operator operation} to this vector and an another one.
     *
     * @param operator lambda operation to be executed
     * @param other    the other vector
     *
     * @return this vector
     */
    public Vector applyOperatorAndSet(@NotNull Operator operator, @NotNull Vector other) {
        if(size != other.size)
            throw new IllegalArgumentException("Both vectors must be equal in size.");

        for(int i = 0; i < size; i++)
            set(i, operator.run(get(i), other.get(i)));

        return this;
    }

    /**
     * Applies a component-wise {@link Operator operation} to this vector and a scalar.
     *
     * @param operator lambda operation to be executed
     * @param scalar   scalar value
     *
     * @return this vector
     */
    public Vector applyOperatorAndSet(@NotNull Operator operator, @NotNull Fraction scalar) {
        for(int i = 0; i < size; i++)
            set(i, operator.run(get(i), scalar));

        return this;
    }

    /**
     * Adds a vector to this one.
     *
     * @param other the other addend
     *
     * @return this vector
     */
    public Vector addAndSet(@NotNull Vector other) {
        return applyOperatorAndSet(Fraction::add, other);
    }

    /**
     * Subtracts a vector from this one.
     *
     * @param other the subtrahend
     *
     * @return this vector
     */
    public Vector subAndSet(@NotNull Vector other) {
        return applyOperatorAndSet(Fraction::sub, other);
    }

    /**
     * Multiplies this vector by another one.
     *
     * @param other the other factor
     *
     * @return this vector
     */
    public Vector mulAndSet(@NotNull Vector other) {
        return applyOperatorAndSet(Fraction::mul, other);
    }

    /**
     * Multiplies this vector by a scalar.
     *
     * @param scalar a scalar factor
     *
     * @return this vector
     */
    public Vector mulAndSet(@NotNull Fraction scalar) {
        return applyOperatorAndSet(Fraction::mul, scalar);
    }

    /**
     * Divides this vector by another one.
     *
     * @param other the divisor
     *
     * @return this vector
     */
    public Vector divAndSet(@NotNull Vector other) {
        return applyOperatorAndSet(Fraction::div, other);
    }

    /**
     * Divides this vector by a scalar.
     *
     * @param scalar a scalar divisor
     *
     * @return this vector
     */
    public Vector divAndSet(@NotNull Fraction scalar) {
        return applyOperatorAndSet(Fraction::div, scalar);
    }

    /**
     * Negates this vector.
     *
     * @return this vector
     */
    public Vector negateAndSet() {
        return applyOperatorAndSet(Fraction::mul, Fraction.MINUS_ONE);
    }

    /**
     * Applies a component-wise {@link Operator operation} to
     * a newly created object using this vector and an another one.
     *
     * @param other    the other vector
     * @param operator lambda operation to be executed
     *
     * @return a separate vector being the result of this operation
     */
    public Vector applyOperator(@NotNull Operator operator, @NotNull Vector other) {
        return new Vector(this).applyOperatorAndSet(operator, other);
    }

    /**
     * Applies a component-wise {@link Operator operation} to
     * a newly created object using this vector and a scalar.
     *
     * @param operator lambda operation to be executed
     * @param scalar   scalar value
     *
     * @return a separate vector being the result of this operation
     */
    public Vector applyOperator(@NotNull Operator operator, @NotNull Fraction scalar) {
        return new Vector(this).applyOperatorAndSet(operator, scalar);
    }

    /**
     * Adds a vector to this one without altering their state.
     *
     * @param other the other addend
     *
     * @return this + other
     */
    public Vector add(@NotNull Vector other) {
        return new Vector(this).addAndSet(other);
    }

    /**
     * Subtracts a vector from this one without altering their state.
     *
     * @param other the subtrahend
     *
     * @return this - other
     */
    public Vector sub(@NotNull Vector other) {
        return new Vector(this).subAndSet(other);
    }

    /**
     * Multiplies this vector by another one without altering their state.
     *
     * @param other the other factor
     *
     * @return this * other
     */
    public Vector mul(@NotNull Vector other) {
        return new Vector(this).mulAndSet(other);
    }

    /**
     * Multiplies this vector by a scalar without altering its state.
     *
     * @param scalar a scalar factor
     *
     * @return this * scalar
     */
    public Vector mul(@NotNull Fraction scalar) {
        return new Vector(this).mulAndSet(scalar);
    }

    /**
     * Divides this vector by another one without altering their state.
     *
     * @param other the divisor
     *
     * @return this / other
     */
    public Vector div(@NotNull Vector other) {
        return new Vector(this).divAndSet(other);
    }

    /**
     * Divides this vector by a scalar without altering its state.
     *
     * @param scalar a scalar divisor
     *
     * @return this / scalar
     */
    public Vector div(@NotNull Fraction scalar) {
        return new Vector(this).divAndSet(scalar);
    }

    /**
     * Negates this vector without altering its state.
     *
     * @return -this
     */
    public Vector negate() {
        return applyOperator(Fraction::mul, Fraction.ONE.negate());
    }

    /**
     * Tells whether this is a zero vector.
     *
     * @return true if all components are zeros
     */
    public boolean isZero() {
        for(int i = 0; i < size; i++) {
            if(get(i).sgn() != 0)
                return false;
        }
        return true;
    }

    /**
     * Computes the dot product of this vector and an another one.
     *
     * @param other the other vector
     *
     * @return this • other
     */
    public Fraction dot(@NotNull Vector other) {
        if(size != other.size)
            throw new IllegalArgumentException("Both vectors must be equal in size.");

        Vector v = mul(other);
        Fraction sum = new Fraction(0);

        for(int i = 0; i < size; i++)
            sum = sum.add(v.get(i));

        return sum;
    }

    /**
     * Computes squared length of this vector.
     *
     * @return this • this
     */
    public Fraction sdot() {
        return dot(this);
    }

    /**
     * Computes the Gram-Schmidt coefficient when projecting onto this vector.
     *
     * @param other the vector that's being projected onto this one
     *
     * @return Gram-Schmidt coefficient
     */
    public Fraction gramSchmidt(@NotNull Vector other) {
        return dot(other).div(sdot());
    }

    /**
     * Projects a vector onto this one.
     *
     * @param other the other vector
     *
     * @return proj<sub>this</sub>other
     */
    public Vector proj(@NotNull Vector other) {
        return mul(gramSchmidt(other));
    }

    /**
     * Returns the number of components in this vector.
     *
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the stride of this vector.
     *
     * <p>Stride determines every how many elements a vector component is located (a in ax + b).
     *
     * @return stride
     */
    public int getStride() {
        return stride;
    }

    /**
     * Returns the offset of this vector.
     *
     * <p>Offset determines the memory offset of every component (b in ax + b).
     *
     * @return offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the underlying array of this vector.
     *
     * @return one-dimensional array
     */
    public Fraction[] getArray() {
        return array;
    }

    private final int size, stride, offset;
    private final Fraction[] array;

    private Vector(int size, int stride, int offset, @NotNull Fraction[] array) {
        this.size = size;
        this.stride = stride;
        this.offset = offset;

        this.array = array;
    }
}