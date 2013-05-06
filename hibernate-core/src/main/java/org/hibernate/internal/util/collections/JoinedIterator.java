/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.internal.util.collections;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An Iterator implementation that wraps other Iterators, and presents them all as one
 * continuous Iterator.  When any method from Iterator is called, we delegate to each
 * wrapped Iterator in turn until all wrapped Iterators are exhausted.
 *
 * @author Gavine King
 * @author Steve Ebersole
 */
public class JoinedIterator<T> implements Iterator<T> {
	private Iterator<T>[] wrappedIterators;

	private int currentIteratorIndex;
	private Iterator<T> currentIterator;
	private Iterator<T> lastUsedIterator;

	@SuppressWarnings("unchecked")
	public JoinedIterator(List<Iterator<T>> wrappedIterators) {
		this( wrappedIterators.toArray( new Iterator[ wrappedIterators.size() ]) );
	}

	public JoinedIterator(Iterator<T>... iteratorsToWrap) {
		if( iteratorsToWrap == null ) {
			throw new NullPointerException( "Iterators to join were null" );
		}
		this.wrappedIterators = iteratorsToWrap;
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
	protected void updateCurrentIterator() {
		if ( currentIterator == null ) {
			if( wrappedIterators.length == 0  ) {
				currentIterator = Collections.<T>emptyList().iterator();
			}
			else {
				currentIterator = wrappedIterators[0];
			}
			// set last used iterator here, in case the user calls remove
			// before calling hasNext() or next() (although they shouldn't)
			lastUsedIterator = currentIterator;
		}

		while (! currentIterator.hasNext() && currentIteratorIndex < wrappedIterators.length - 1) {
			currentIteratorIndex++;
			currentIterator = wrappedIterators[currentIteratorIndex];
		}
	}

}
