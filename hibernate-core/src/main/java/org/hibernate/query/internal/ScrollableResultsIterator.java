/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.Incubating;
import org.hibernate.ScrollableResults;
import org.hibernate.query.spi.CloseableIterator;

/**
 * @author Steve Ebersole
 *
 * @since 5.2
 */
@Incubating
public class ScrollableResultsIterator<T> implements CloseableIterator<T> {
	private final ScrollableResults<T> scrollableResults;

	public ScrollableResultsIterator(ScrollableResults<T> scrollableResults) {
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
		return scrollableResults.get();
	}
}
