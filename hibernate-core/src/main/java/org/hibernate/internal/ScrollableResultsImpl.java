/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Standard ScrollableResults implementation.
 *
 * @author Gavin King
 */
public class ScrollableResultsImpl<R> extends AbstractScrollableResults<R> {
	private R currentRow;

	public ScrollableResultsImpl(
			JdbcValues jdbcValues,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			SharedSessionContractImplementor persistenceContext) {
		super(
				jdbcValues,
				processingOptions,
				jdbcValuesSourceProcessingState,
				rowProcessingState,
				rowReader,
				persistenceContext
		);
	}

	@Override
	protected R getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean next() {
		final boolean result = getRowProcessingState().next();
		prepareCurrentRow( result );
		return result;
	}

	@Override
	public boolean previous() {
		final boolean result = getRowProcessingState().previous();
		prepareCurrentRow( result );
		return result;
	}

	@Override
	public boolean scroll(int i) {
		final boolean hasResult = getRowProcessingState().scroll( i );
		prepareCurrentRow( hasResult );
		return hasResult;
	}

	@Override
	public boolean position(int position) {
		final boolean hasResult = getRowProcessingState().position( position );
		prepareCurrentRow( hasResult );
		return hasResult;
	}

	@Override
	public boolean first() {
		final boolean hasResult = getRowProcessingState().first();
		prepareCurrentRow( hasResult );
		return hasResult;
	}

	@Override
	public boolean last() {
		final boolean hasResult = getRowProcessingState().last();
		prepareCurrentRow( hasResult );
		return hasResult;

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			final boolean result = getResultSet().last();
//			prepareCurrentRow( result );
//			return result;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "could not advance using last()" );
//		}
	}

	@Override
	public void afterLast() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			getResultSet().afterLast();
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "exception calling afterLast()" );
//		}
	}

	@Override
	public void beforeFirst() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			getResultSet().beforeFirst();
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "exception calling beforeFirst()" );
//		}
	}

	@Override
	public boolean isFirst() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			return getResultSet().isFirst();
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "exception calling isFirst()" );
//		}
	}

	@Override
	public boolean isLast() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			return getResultSet().isLast();
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "exception calling isLast()" );
//		}
	}

	@Override
	public int getRowNumber() throws HibernateException {
		return getRowProcessingState().getPosition();
	}

	@Override
	public boolean setRowNumber(int rowNumber) throws HibernateException {
		return position( rowNumber );
	}

	private void prepareCurrentRow(boolean underlyingScrollSuccessful) {
		if ( !underlyingScrollSuccessful ) {
			currentRow = null;
			return;
		}

		currentRow = getRowReader().readRow(
				getRowProcessingState(),
				getProcessingOptions()
		);

		afterScrollOperation();
	}

}
