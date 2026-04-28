/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.scrollable;

import java.util.List;

import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * Simple list-backed {@link ScrollableResultsImplementor} used when a query is
 * fully materialized and any limit/offset is applied in memory.
 */
public class ListBackedScrollableResults<R> implements ScrollableResultsImplementor<R> {
	private final List<R> results;
	private int currentIndex = -1;
	private boolean closed;

	public ListBackedScrollableResults(List<R> results) {
		this.results = results;
	}

	@Override
	public R get() {
		if ( closed ) {
			throw new IllegalStateException( "ScrollableResults is closed" );
		}
		return currentIndex >= 0 && currentIndex < results.size()
				? results.get( currentIndex )
				: null;
	}

	@Override
	public void close() {
		closed = true;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean next() {
		return moveTo( currentIndex + 1 );
	}

	@Override
	public boolean previous() {
		return moveTo( currentIndex - 1 );
	}

	@Override
	public boolean scroll(int positions) {
		return positions == 0
				? currentIndex >= 0 && currentIndex < results.size()
				: moveTo( currentIndex + positions );
	}

	@Override
	public boolean position(int position) {
		if ( position > 0 ) {
			return moveTo( position - 1 );
		}
		else if ( position < 0 ) {
			return moveTo( results.size() + position );
		}
		else {
			beforeFirst();
			return false;
		}
	}

	@Override
	public int getPosition() {
		return currentIndex >= 0 && currentIndex < results.size()
				? currentIndex + 1
				: 0;
	}

	@Override
	public boolean last() {
		return moveTo( results.size() - 1 );
	}

	@Override
	public boolean first() {
		return moveTo( 0 );
	}

	@Override
	public void beforeFirst() {
		currentIndex = -1;
	}

	@Override
	public void afterLast() {
		currentIndex = results.size();
	}

	@Override
	public boolean isFirst() {
		return currentIndex == 0 && !results.isEmpty();
	}

	@Override
	public boolean isLast() {
		return currentIndex == results.size() - 1 && !results.isEmpty();
	}

	@Override
	public int getRowNumber() {
		return currentIndex >= 0 && currentIndex < results.size()
				? currentIndex
				: -1;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		return position( rowNumber );
	}

	@Override
	public void setFetchSize(int fetchSize) {
	}

	private boolean moveTo(int targetIndex) {
		if ( targetIndex < 0 ) {
			beforeFirst();
			return false;
		}
		else if ( targetIndex >= results.size() ) {
			afterLast();
			return false;
		}
		else {
			currentIndex = targetIndex;
			return true;
		}
	}
}
