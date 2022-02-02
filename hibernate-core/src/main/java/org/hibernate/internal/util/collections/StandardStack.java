/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A general-purpose stack impl supporting null values.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class StandardStack<T> implements Stack<T> {

	private ArrayDeque<T> internalStack;
	private static final Object NULL_TOKEN = new Object();

	public StandardStack() {
	}

	public StandardStack(T initial) {
		stackInstanceExpected().addFirst( initial );
	}

	@Override
	public void push(T newCurrent) {
		T toStore = newCurrent;
		if ( newCurrent == null ) {
			toStore = (T) NULL_TOKEN;
		}
		stackInstanceExpected().addFirst( toStore );
	}

	private Deque<T> stackInstanceExpected() {
		if ( internalStack == null ) {
			//"7" picked to use 8, but skipping the odd initialCapacity method
			internalStack = new ArrayDeque<>( 7 );
		}
		return internalStack;
	}

	@Override
	public T pop() {
		return convert( stackInstanceExpected().removeFirst() );
	}

	private T convert(final Object internalStoredObject) {
		if ( internalStoredObject == NULL_TOKEN ) {
			return null;
		}
		return (T) internalStoredObject;
	}

	@Override
	public T getCurrent() {
		if ( internalStack == null ) {
			return null;
		}
		return convert( internalStack.peek() );
	}

	@Override
	public int depth() {
		if ( internalStack == null ) {
			return 0;
		}
		return internalStack.size();
	}

	@Override
	public boolean isEmpty() {
		if ( internalStack == null ) {
			return true;
		}
		return internalStack.isEmpty();
	}

	@Override
	public void clear() {
		if ( internalStack != null ) {
			internalStack.clear();
		}
	}

	@Override
	public void visitRootFirst(Consumer<T> action) {
		if ( internalStack == null ) {
			return;
		}
		final Iterator<T> iterator = internalStack.descendingIterator();
		while ( iterator.hasNext() ) {
			action.accept( iterator.next() );
		}
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		if ( internalStack == null ) {
			return null;
		}
		for (T t : internalStack) {
			final X result = function.apply(t);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

}
