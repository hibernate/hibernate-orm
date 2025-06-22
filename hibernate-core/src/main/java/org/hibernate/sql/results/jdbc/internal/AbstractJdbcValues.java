/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcValues implements JdbcValues {

	@Override
	public final boolean next(RowProcessingState rowProcessingState) {
		return processNext( rowProcessingState );
	}

	protected abstract boolean processNext(RowProcessingState rowProcessingState);

	@Override
	public boolean previous(RowProcessingState rowProcessingState) {
		// NOTE : we do not even bother interacting with the query-cache put manager because
		//		this method is implicitly related to scrolling and caching of scrolled results
		//		is not supported
		return processPrevious( rowProcessingState );
	}

	protected abstract boolean processPrevious(RowProcessingState rowProcessingState);

	@Override
	public boolean scroll(int numberOfRows, RowProcessingState rowProcessingState) {
		// NOTE : we do not even bother interacting with the query-cache put manager because
		//		this method is implicitly related to scrolling and caching of scrolled results
		//		is not supported
		return processScroll( numberOfRows, rowProcessingState );
	}

	protected abstract boolean processScroll(int numberOfRows, RowProcessingState rowProcessingState);

	@Override
	public boolean position(int position, RowProcessingState rowProcessingState) {
		// NOTE : we do not even bother interacting with the query-cache put manager because
		//		this method is implicitly related to scrolling and caching of scrolled results
		//		is not supported
		return processPosition( position, rowProcessingState );
	}

	protected abstract boolean processPosition(int position, RowProcessingState rowProcessingState);
}
