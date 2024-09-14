/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.lang.reflect.Array;
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
public final class StandardStack<T> implements Stack<T> {
	private T[] elements;
	private int top = 0;

	private Class<T> type;

	public StandardStack(Class<T> type) {
		this.type = type;
	}

	public StandardStack(Class<T> type, T initial) {
		this( type );
		push( initial );
	}

	@SuppressWarnings("unchecked")
	private void init() {
		elements = (T[]) Array.newInstance( type, 8 );
		type = null;
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
		T e = elements[--top];
		elements[top] = null;
		return e;
	}

	@Override
	public T getCurrent() {
		if ( isEmpty() ) {
			return null;
		}
		return elements[top - 1];
	}

	@Override
	public T peek(int offsetFromTop) {
		if ( isEmpty() ) {
			return null;
		}
		return elements[top - offsetFromTop - 1];
	}

	@Override
	public T getRoot() {
		if ( isEmpty() ) {
			return null;
		}
		return elements[0];
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
		for ( int i = 0; i < top; i++ ) {
			elements[i] = null;
		}
		top = 0;
	}

	@Override
	public void visitRootFirst(Consumer<T> action) {
		for ( int i = 0; i < top; i++ ) {
			action.accept( elements[i] );
		}
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		for ( int i = top - 1; i >= 0; i-- ) {
			final X result = function.apply( elements[i] );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	public <X, Y> X findCurrentFirstWithParameter(Y parameter, BiFunction<T, Y, X> biFunction) {
		for ( int i = top - 1; i >= 0; i-- ) {
			final X result = biFunction.apply( elements[i], parameter );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private void grow() {
		final int oldCapacity = elements.length;
		final int jump = ( oldCapacity < 64 ) ? ( oldCapacity + 2 ) : ( oldCapacity >> 1 );
		elements = Arrays.copyOf( elements, oldCapacity + jump );
	}
}
