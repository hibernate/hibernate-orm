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
	@SuppressWarnings("unchecked")
	private final T nullMarker = (T) new Object();

	private T current;

	private LinkedList<T> internalStack;

	public StandardStack() {
	}

	public StandardStack(T initial) {
		current = initial;
	}

	@Override
	public void push(T newCurrent) {
		if ( newCurrent == null ) {
			newCurrent = nullMarker;
		}

		if ( current != null ) {
			if ( internalStack == null ) {
				internalStack = new LinkedList<>();
			}
			internalStack.addFirst( current );
		}

		current = newCurrent;
	}

	@Override
	public T pop() {
		final T popped = this.current;
		if ( internalStack == null || internalStack.isEmpty() ) {
			this.current = null;
		}
		else {
			this.current = internalStack.removeFirst();
		}

		return popped == nullMarker ? null : popped;
	}

	@Override
	public T getCurrent() {
		return current == nullMarker ? null : current;
	}

	@Override
	public T getPrevious() {
		if ( current != null && internalStack != null ) {
			final T previous = internalStack.getFirst();
			return previous == nullMarker ? null : previous;
		}

		// otherwise...
		return null;
	}

	@Override
	public int depth() {
		if ( current == null ) {
			return 0;
		}
		else if ( internalStack == null || internalStack.isEmpty() ) {
			return 1;
		}
		else {
			return internalStack.size() + 1;
		}
	}

	@Override
	public boolean isEmpty() {
		return current == null;
	}

	@Override
	public void clear() {
		current = null;
		if ( internalStack != null ) {
			internalStack.clear();
		}
	}

	@Override
	public void visitCurrentFirst(Consumer<T> action) {
		if ( current != null ) {
			action.accept( current );
			if ( internalStack != null ) {
				internalStack.forEach( action );
			}
		}
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		if ( current != null ) {
			{
				final X result = function.apply( current );

				if ( result != null ) {
					return result;
				}
			}

			if ( internalStack != null ) {
				for ( T t : internalStack ) {
					final X result = function.apply( t );
					if ( result != null ) {
						return result;
					}
				}
			}
		}

		return null;
	}
}
