/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
public class ScrollableResultsIterator<T> implements CloseableIterator<T> {
	private final ScrollableResultsImplementor<T> scrollableResults;

	public ScrollableResultsIterator(ScrollableResultsImplementor<T> scrollableResults) {
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
	public T next() {
		return (T) scrollableResults.get();
	}
}
