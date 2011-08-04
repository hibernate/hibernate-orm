/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal.util.collections;

import java.util.Iterator;
import java.util.List;


/**
 * An JoinedIterable is an Iterable that wraps a number of Iterables.
 *
 * This class makes multiple iterables look like one to the caller.
 * When any method from the Iterator interface is called on the
 * Iterator object returned by {@link #iterator()}, the JoinedIterable
 * will delegate to a single underlying Iterator. The JoinedIterable will
 * invoke the iterator on each Iterable, in sequence, until all Iterators
 * are exhausted.
 *
 * @author Gail Badner (adapted from JoinedIterator)
 */
public class JoinedIterable<T> implements Iterable<T> {
	private final TypeSafeJoinedIterator<T> iterator;

	public JoinedIterable(List<Iterable<T>> iterables) {
		if ( iterables == null ) {
			throw new NullPointerException( "Unexpected null iterables argument" );
		}
		iterator = new TypeSafeJoinedIterator<T>( iterables );
	}

	public Iterator<T> iterator() {
		return iterator;
	}

	private class TypeSafeJoinedIterator<T> implements Iterator<T> {

		// wrapped iterators
		private List<Iterable<T>> iterables;

		// index of current iterator in the wrapped iterators array
		private int currentIterableIndex;

		// the current iterator
		private Iterator<T> currentIterator;

		// the last used iterator
		private Iterator<T> lastUsedIterator;

		public TypeSafeJoinedIterator(List<Iterable<T>> iterables) {
			this.iterables = iterables;
		}

		public boolean hasNext() {
			updateCurrentIterator();
			return currentIterator.hasNext();
		}

		public T next() {
			updateCurrentIterator();
			return currentIterator.next();
		}

		public void remove() {
			updateCurrentIterator();
			lastUsedIterator.remove();
		}

		// call this before any Iterator method to make sure that the current Iterator
		// is not exhausted
		@SuppressWarnings( {"unchecked"})
		protected void updateCurrentIterator() {

			if ( currentIterator == null) {
				if( iterables.size() == 0  ) {
					currentIterator = EmptyIterator.INSTANCE;
				}
				else {
					currentIterator = iterables.get( 0 ).iterator();
				}
				// set last used iterator here, in case the user calls remove
				// before calling hasNext() or next() (although they shouldn't)
				lastUsedIterator = currentIterator;
			}

			while (! currentIterator.hasNext() && currentIterableIndex < iterables.size() - 1) {
				currentIterableIndex++;
				currentIterator = iterables.get( currentIterableIndex ).iterator();
			}
		}
	}
}
