/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

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

}
