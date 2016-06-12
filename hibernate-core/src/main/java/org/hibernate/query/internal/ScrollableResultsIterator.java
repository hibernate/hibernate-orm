/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.Incubating;
import org.hibernate.query.spi.CloseableIterator;
import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * @author Steve Ebersole
 *
 * @since 5.2
 */
@Incubating
class ScrollableResultsIterator<T> implements CloseableIterator {
	private final ScrollableResultsImplementor scrollableResults;

	ScrollableResultsIterator(ScrollableResultsImplementor scrollableResults) {
		this.scrollableResults = scrollableResults;
	}

	@Override
	public void close() {
		scrollableResults.close();
	}

	@Override
	public boolean hasNext() {
		return !scrollableResults.isClosed() && scrollableResults.next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T next() {
		if ( scrollableResults.getNumberOfTypes() == 1 ) {
			return (T) scrollableResults.get()[0];
		}
		else {
			return (T) scrollableResults.get();
		}
	}
}
