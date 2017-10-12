/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public class FilterableNavigableSpliterator<N extends Navigable<?>> implements Spliterator<N> {
	public static final int CHARACTERISTICS = DISTINCT & NONNULL & IMMUTABLE;

	private final List<Navigable<?>> listOfNavigables;
	private final Class<N> filterType;

	private Iterator iterator;
	private boolean reachedEnd;

	public FilterableNavigableSpliterator(
			List<Navigable<?>> listOfNavigables,
			Class<N> filterType) {
		this.listOfNavigables = listOfNavigables;
		this.filterType = filterType;
	}

	@Override
	public boolean tryAdvance(Consumer<? super N> action) {
//		System.out.println("try advance");
		if ( reachedEnd ) {
			// should indicate we are past the end of all sub-spliterators
			return false;
		}

		if ( iterator == null ) {
			iterator = listOfNavigables.iterator();
		}

		return internalTryAdvance( action );
	}

	@SuppressWarnings("PointlessBooleanExpression")
	private boolean internalTryAdvance(Consumer<? super N> action) {
		final N nextMatch = findNextMatch();

		if ( reachedEnd ) {
			assert nextMatch == null;
			return false;
		}

		action.accept( nextMatch );

		return true;
	}

	@SuppressWarnings("unchecked")
	private N findNextMatch() {
		assert iterator != null;

		while ( iterator.hasNext() ) {
			final N next = (N) iterator.next();
			if ( filterType == null || filterType.isInstance( next ) ) {
				return next;
			}
		}

		// if we get here, the iterator is finished - `! hasNext()`
		reachedEnd = true;
		iterator = null;

		return null;
	}

	@Override
	public Spliterator<N> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return listOfNavigables.size();
	}

	@Override
	public int characteristics() {
		return CHARACTERISTICS;
	}
}
