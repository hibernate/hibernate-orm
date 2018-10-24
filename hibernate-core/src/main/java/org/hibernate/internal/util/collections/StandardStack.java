/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A general-purpose stack impl.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public class StandardStack<T> implements Stack<T> {
	private LinkedList<T> internalStack = new LinkedList<>();

	public StandardStack() {
	}

	public StandardStack(T initial) {
		internalStack.add( initial );
	}

	@Override
	public void push(T newCurrent) {
		internalStack.addFirst( newCurrent );
	}

	@Override
	public T pop() {
		return internalStack.removeFirst();
	}

	@Override
	public T getCurrent() {
		return internalStack.peek();
	}

	@Override
	public T getPrevious() {
		if ( internalStack.size() < 2 ) {
			return null;
		}
		return internalStack.get( internalStack.size() - 2 );
	}

	@Override
	public int depth() {
		return internalStack.size();
	}

	@Override
	public boolean isEmpty() {
		return internalStack.isEmpty();
	}

	@Override
	public void clear() {
		internalStack.clear();
	}

	@Override
	public void visitCurrentFirst(Consumer<T> action) {
		internalStack.forEach( action );
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		for ( T t : internalStack ) {
			final X result = function.apply( t );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
