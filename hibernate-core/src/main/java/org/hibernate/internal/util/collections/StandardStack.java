/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A general-purpose stack impl supporting null values.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 * @author Marco Belladelli
 */
@SuppressWarnings("unchecked")
public final class StandardStack<T> implements Stack<T> {

	private Object[] elements;
	private int top = 0;

	public StandardStack() {
	}

	public StandardStack(T initialValue) {
		push( initialValue );
	}

	/**
	 * @deprecated use the default constructor instead
	 */
	@Deprecated(forRemoval = true)
	public StandardStack(Class<T> type) {
	}

	/**
	 * @deprecated use {@link #StandardStack(Object)} instead.
	 */
	@Deprecated(forRemoval = true)
	public StandardStack(Class<T> type, T initial) {
		push( initial );
	}

	private void init() {
		elements = new Object[8];
	}

	@Override
	public void push(T e) {
		if ( elements == null ) {
			init();
		}
		if ( top == elements.length ) {
			grow();
		}
		elements[top++] = e;
	}

	@Override
	public T pop() {
		if ( isEmpty() ) {
			throw new NoSuchElementException();
		}
		T e = (T) elements[--top];
		elements[top] = null;
		return e;
	}

	@Override
	public T getCurrent() {
		if ( isEmpty() ) {
			return null;
		}
		return (T) elements[top - 1];
	}

	@Override
	public T peek(int offsetFromTop) {
		if ( isEmpty() ) {
			return null;
		}
		return (T) elements[top - offsetFromTop - 1];
	}

	@Override
	public T getRoot() {
		if ( isEmpty() ) {
			return null;
		}
		return (T) elements[0];
	}

	@Override
	public int depth() {
		return top;
	}

	@Override
	public boolean isEmpty() {
		return top == 0;
	}

	@Override
	public void clear() {
		if ( elements != null ) {
			Arrays.fill( elements, 0, top, null );
		}
		top = 0;
	}

	@Override
	public void visitRootFirst(Consumer<T> action) {
		for ( int i = 0; i < top; i++ ) {
			action.accept( (T) elements[i] );
		}
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		for ( int i = top - 1; i >= 0; i-- ) {
			final X result = function.apply( (T) elements[i] );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	public <X, Y> X findCurrentFirstWithParameter(Y parameter, BiFunction<T, Y, X> biFunction) {
		for ( int i = top - 1; i >= 0; i-- ) {
			final X result = biFunction.apply( (T) elements[i], parameter );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private void grow() {
		final int oldCapacity = elements.length;
		final int jump = ( oldCapacity < 64 ) ? ( oldCapacity + 2 ) : ( oldCapacity >> 1 );
		final Object[] newElements = Arrays.copyOf( elements, oldCapacity + jump );
		// Prevents GC nepotism on the old array elements, see HHH-19047
		Arrays.fill( elements, null );
		elements = newElements;
	}

}
