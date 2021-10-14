/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A general-purpose stack impl.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public final class StandardStack<T> implements Stack<T> {

	private ArrayDeque<T> internalStack;

	public StandardStack() {
	}

	@Override
	public void push(T newCurrent) {
		stackInstanceExpected().addFirst( newCurrent );
	}

	private Deque<T> stackInstanceExpected() {
		if ( internalStack == null ) {
			//"7" picked to use 8, but skipping the odd initialCapacity method
			internalStack = new ArrayDeque<>(7);
		}
		return internalStack;
	}

	@Override
	public T pop() {
		return stackInstanceExpected().removeFirst();
	}

	@Override
	public T getCurrent() {
		if ( internalStack == null ) {
			return null;
		}
		return internalStack.peek();
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

}
