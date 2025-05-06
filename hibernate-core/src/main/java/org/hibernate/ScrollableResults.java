/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.query.Query;

/**
 * A result iterator that allows moving around within the results by
 * arbitrary increments.
 *
 * @apiNote The {@link Query} / {@link ScrollableResults} pattern is
 * very similar to the JDBC {@link java.sql.PreparedStatement} /
 * {@link java.sql.ResultSet} pattern and so the semantics of methods
 * of this interface are similar to the similarly-named methods of
 * {@code ResultSet}.
 *
 * @see org.hibernate.query.SelectionQuery#scroll()
 *
 * @author Gavin King
 */
public interface ScrollableResults<R> extends AutoCloseable {
	/**
	 * Get the current row of results.
	 *
	 * @return The array of results
	 */
	R get();

	/**
	 * Release resources immediately.
	 */
	void close();

	/**
	 * @return {@code true} if {@link #close()} was already called
	 */
	boolean isClosed();

	/**
	 * Advance to the next result.
	 *
	 * @return {@code true} if there is another result
	 */
	boolean next();

	/**
	 * Retreat to the previous result.
	 *
	 * @return {@code true} if there is a previous result
	 */
	boolean previous();

	/**
	 * Scroll the specified number of positions from the current
	 * position.
	 *
	 * @param positions a positive (forward) or negative (backward)
	 *                  number of positions
	 *
	 * @return {@code true} if there is a result at the new location
	 */
	boolean scroll(int positions);

	/**
	 * Moves the result cursor to the specified position. The index
	 * may be a positive value, and the position may be reached by
	 * counting forward from the first result at position {@code 1},
	 * or it may be a negative value, so that the position may be
	 * reached by counting backward from the last result at position
	 * {@code -1}.
	 *
	 * @param position an absolute positive (from the start) or
	 *                 negative (from the end) position within the
	 *                 query results
	 *
	 * @return {@code true} if there is a result at the new location
	 */
	boolean position(int position);

	/**
	 * The current position within the query results. The first
	 * query result, if any, is at position {@code 1}. An empty
	 * or newly-created instance has position {@code 0}.
	 *
	 * @return the current position, a positive integer index
	 *         starting at {@code 1}, or {@code 0} if this
	 *         instance is empty or newly-created
	 *
	 * @since 7.0
	 */
	int getPosition();

	/**
	 * Go to the last result.
	 *
	 * @return {@code true} if there are any results
	 */
	boolean last();

	/**
	 * Go to the first result.
	 *
	 * @return {@code true} if there are any results
	 */
	boolean first();

	/**
	 * Go to a location just before first result.
	 * <p>
	 * This is the location of the cursor on a newly returned
	 * scrollable result.
	 */
	void beforeFirst();

	/**
	 * Go to a location just after the last result.
	 */
	void afterLast();

	/**
	 * Is this the first result?
	 *
	 * @return {@code true} if this is the first row of results,
	 *         otherwise {@code false}
	 */
	boolean isFirst();

	/**
	 * Is this the last result?
	 *
	 * @return {@code true} if this is the last row of results.
	 */
	boolean isLast();

	/**
	 * Get the current position in the results, with the first
	 * position labelled as row number {@code 0}. That is, this
	 * operation returns {@link #getPosition() position-1}.
	 *
	 * @return The current position number, numbered from {@code 0};
	 *         {@code -1} indicates that there is no current row
	 *
	 * @deprecated Use {@link #getPosition()}
	 */
	@Deprecated(since = "7", forRemoval = true)
	int getRowNumber();

	/**
	 * Set the current position in the result set, with the first
	 * position labelled as row number {@code 1}, and the last
	 * position labelled as row number {@code -1}. Results may be
	 * numbered from the first result (using a positive position)
	 * or backward from the last result (using a negative position).
	 *
	 * @param rowNumber the row number. A positive number indicates
	 *                  a value numbered from the first row; a
	 *                  negative number indicates a value numbered
	 *                  from the last row.
	 *
	 * @return true if there is a row at that row number
	 *
	 * @deprecated Use {@link #position(int)}
	 */
	@Deprecated(since = "7", forRemoval = true)
	boolean setRowNumber(int rowNumber);

	/**
	 * Gives the JDBC driver a hint as to the number of rows that
	 * should be fetched from the database when more rows are needed.
	 * If {@code 0}, the JDBC driver's default setting will be used.
	 *
	 * @see java.sql.ResultSet#setFetchSize(int)
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_FETCH_SIZE
	 *
	 * @since 6.1.2
	 */
	void setFetchSize(int fetchSize);
}
