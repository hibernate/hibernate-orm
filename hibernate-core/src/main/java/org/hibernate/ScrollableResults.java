/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Closeable;

import org.hibernate.query.Query;

/**
 * A result iterator that allows moving around within the results
 * by arbitrary increments. The <tt>Query</tt> / <tt>ScrollableResults</tt>
 * pattern is very similar to the JDBC <tt>PreparedStatement</tt>/
 * <tt>ResultSet</tt> pattern and the semantics of methods of this interface
 * are similar to the similarly named methods on <tt>ResultSet</tt>.<br>
 * <br>
 * Contrary to JDBC, columns of results are numbered from zero.
 *
 * @see Query#scroll()
 *
 * @author Gavin King
 */
public interface ScrollableResults<R> extends AutoCloseable, Closeable {
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
	 * Scroll the specified number of positions from the current position.
	 *
	 * @param positions a positive (forward) or negative (backward) number of rows
	 *
	 * @return {@code true} if there is a result at the new location
	 */
	boolean scroll(int positions);

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
	 * Go to a location just before first result,  This is the location of the cursor on a newly returned
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
	 * @return {@code true} if this is the first row of results, otherwise {@code false}
	 */
	boolean isFirst();

	/**
	 * Is this the last result?
	 *
	 * @return {@code true} if this is the last row of results.
	 */
	boolean isLast();

	/**
	 * Get the current position in the results. The first position is number 0 (unlike JDBC).
	 *
	 * @return The current position number, numbered from 0; -1 indicates that there is no current row
	 */
	int getRowNumber();

	/**
	 * Set the current position in the result set.  Can be numbered from the first position (positive number) or
	 * the last row (negative number).
	 *
	 * @param rowNumber the row number.  A positive number indicates a value numbered from the first row; a
	 * negative number indicates a value numbered from the last row.
	 *
	 * @return true if there is a row at that row number
	 */
	boolean setRowNumber(int rowNumber);
}
