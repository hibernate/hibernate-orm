/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.values.JdbcValues;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
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
	public boolean scroll(int i) {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			final boolean result = getResultSet().relative( i );
//			prepareCurrentRow( result );
//			return result;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "could not advance using scroll()" );
//		}
	}

	protected JDBCException convert(SQLException sqle, String message) {
		return getPersistenceContext().getJdbcServices().getSqlExceptionHelper().convert( sqle, message );
	}

	@Override
	public boolean first() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			final boolean result = getResultSet().first();
//			prepareCurrentRow( result );
//			return result;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "could not advance using first()" );
//		}
	}

	@Override
	public boolean last() {
		throw new NotYetImplementedFor6Exception();

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
	public boolean next() {
		try {
			final boolean result = getJdbcValues().next( getRowProcessingState() );
			prepareCurrentRow( result );
			return result;
		}
		catch (SQLException sqle) {
			throw convert( sqle, "could not advance using next()" );
		}
	}

	@Override
	public boolean previous() {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			final boolean result = getResultSet().previous();
//			prepareCurrentRow( result );
//			return result;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "could not advance using previous()" );
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
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		try {
//			return getResultSet().getRow() - 1;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "exception calling getRow()" );
//		}
	}

	@Override
	public boolean setRowNumber(int rowNumber) throws HibernateException {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : need these scrollable ResultSet "re-positioning"-style methods on the JdbcValues stuff

//		if ( rowNumber >= 0 ) {
//			rowNumber++;
//		}
//
//		try {
//			final boolean result = getResultSet().absolute( rowNumber );
//			prepareCurrentRow( result );
//			return result;
//		}
//		catch (SQLException sqle) {
//			throw convert( sqle, "could not advance using absolute()" );
//		}
	}

	private void prepareCurrentRow(boolean underlyingScrollSuccessful) {
		if ( !underlyingScrollSuccessful ) {
			currentRow = null;
			return;
		}

		try {
			currentRow = getRowReader().readRow(
					getRowProcessingState(),
					getProcessingOptions()
			);
		}
		catch (SQLException e) {
			throw convert( e, "Unable to read row as part of ScrollableResult handling" );
		}

		afterScrollOperation();
	}

}
