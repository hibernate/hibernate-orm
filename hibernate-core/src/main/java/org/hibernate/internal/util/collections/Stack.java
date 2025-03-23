/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Incubating;

/**
 * Stack implementation exposing useful methods for Hibernate needs.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public interface Stack<T> {
	/**
	 * Push the new element on the top of the stack
	 */
	void push(T newCurrent);

	/**
	 * Pop (remove and return) the current element off the stack
	 */
	T pop();

	/**
	 * The element currently at the top of the stack
	 */
	T getCurrent();

	/**
	 * The element at the given offset, relative to the top of the stack
	 */
	T peek(int offsetFromTop);

	/**
	 * The element currently at the bottom of the stack
	 */
	T getRoot();

	/**
	 * How many elements are currently on the stack?
	 */
	int depth();

	/**
	 * Are there no elements currently in the stack?
	 */
	boolean isEmpty();

	/**
	 * Remove all elements from the stack
	 */
	void clear();

	/**
	 * Visit all elements in the stack, starting with the root and working "forward"
	 */
	void visitRootFirst(Consumer<T> action);

	/**
	 * Find an element on the stack and return a value.  The first non-null element
	 * returned from `action` stops the iteration and is returned from here
	 */
	<X> X findCurrentFirst(Function<T, X> action);

	/**
	 * Runs a function on each couple defined as {Y,T} in which Y
	 * is fixed, and T is each instance of this stack.
	 * Not all Ts might be presented as the iteration is interrupted as
	 * soon as the first non-null value is returned by the (bi)function,
	 * which is then returned from this function.
	 * @param parameter a parameter to be passed to the (bi)funtion
	 * @param biFunction a (bi)function taking as input parameter Y and each of the T
	 * from the current stack, and return type of X.
	 * @return the first non-null result by the function, or null if no matches.
	 * @param <X> the return type of the function
	 * @param <Y> the type of the fixed parameter
	 */
	@Incubating
	<X,Y> X findCurrentFirstWithParameter(Y parameter, BiFunction<T, Y, X> biFunction);

}
