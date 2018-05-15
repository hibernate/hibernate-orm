/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.values.JdbcValues;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Implementation of ScrollableResults which can handle collection fetches.
 *
 * @author Steve Ebersole
 */
public class FetchingScrollableResultsImpl<R> extends AbstractScrollableResults<R> {
	private R currentRow;

	private int currentPosition;
	private Integer maxPosition;

	public FetchingScrollableResultsImpl(
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
		this.maxPosition = jdbcValuesSourceProcessingState.getQueryOptions().getEffectiveLimit().getMaxRows();
	}

	@Override
	protected R getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean scroll(int i) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public boolean next() {
		throw new NotYetImplementedFor6Exception();

//		if ( maxPosition != null && maxPosition <= currentPosition ) {
//			currentRow = null;
//			currentPosition = maxPosition + 1;
//			return false;
//		}
//
//		if ( isResultSetEmpty() ) {
//			currentRow = null;
//			currentPosition = 0;
//			return false;
//		}
//
//		final Object row = getLoader().loadSequentialRowsForward(
//				getResultSet(),
//				getSession(),
//				getQueryParameters(),
//				true
//		);
//
//
//		final boolean afterLast;
//		try {
//			afterLast = getResultSet().isAfterLast();
//		}
//		catch (SQLException e) {
//			throw getSession().getFactory().getSQLExceptionHelper().convert(
//					e,
//					"exception calling isAfterLast()"
//			);
//		}
//
//		currentPosition++;
//		currentRow = new Object[] {row};
//
//		if ( afterLast ) {
//			if ( maxPosition == null ) {
//				// we just hit the last position
//				maxPosition = currentPosition;
//			}
//		}
//
//		afterScrollOperation();
//
//		return true;
	}

	@Override
	public boolean previous() {
		throw new NotYetImplementedFor6Exception();

//		if ( currentPosition <= 1 ) {
//			currentPosition = 0;
//			currentRow = null;
//			return false;
//		}
//
//		final Object loadResult = getLoader().loadSequentialRowsReverse(
//				getResultSet(),
//				getSession(),
//				getQueryParameters(),
//				false,
//				( maxPosition != null && currentPosition > maxPosition )
//		);
//
//		currentRow = new Object[] {loadResult};
//		currentPosition--;
//
//		afterScrollOperation();
//
//		return true;
//	}
//
//	@Override
//	public boolean scroll(int positions) {
//		boolean more = false;
//		if ( positions > 0 ) {
//			// scroll ahead
//			for ( int i = 0; i < positions; i++ ) {
//				more = next();
//				if ( !more ) {
//					break;
//				}
//			}
//		}
//		else if ( positions < 0 ) {
//			// scroll backward
//			for ( int i = 0; i < ( 0 - positions ); i++ ) {
//				more = previous();
//				if ( !more ) {
//					break;
//				}
//			}
//		}
//		else {
//			throw new HibernateException( "scroll(0) not valid" );
//		}
//
//		afterScrollOperation();
//
//		return more;
	}

	@Override
	public boolean last() {
		throw new NotYetImplementedFor6Exception();

//		boolean more = false;
//		if ( maxPosition != null ) {
//			if ( currentPosition > maxPosition ) {
//				more = previous();
//			}
//			for ( int i = currentPosition; i < maxPosition; i++ ) {
//				more = next();
//			}
//		}
//		else {
//			try {
//				if ( isResultSetEmpty() || getResultSet().isAfterLast() ) {
//					// should not be able to reach last without maxPosition being set
//					// unless there are no results
//					return false;
//				}
//
//				while ( !getResultSet().isAfterLast() ) {
//					more = next();
//				}
//			}
//			catch (SQLException e) {
//				throw getSession().getFactory().getSQLExceptionHelper().convert(
//						e,
//						"exception calling isAfterLast()"
//				);
//			}
//		}
//
//		afterScrollOperation();
//
//		return more;
	}

	@Override
	public boolean first() {
		beforeFirst();
		boolean more = next();

		afterScrollOperation();

		return more;
	}

	@Override
	public void beforeFirst() {
		throw new NotYetImplementedFor6Exception();

//		try {
//			getResultSet().beforeFirst();
//		}
//		catch (SQLException e) {
//			throw getSession().getFactory().getSQLExceptionHelper().convert(
//					e,
//					"exception calling beforeFirst()"
//			);
//		}
//		currentRow = null;
//		currentPosition = 0;
	}

	@Override
	public void afterLast() {
		// TODO : not sure the best way to handle this.
		// The non-performant way :
		last();
		next();
		afterScrollOperation();
	}

	@Override
	public boolean isFirst() {
		return currentPosition == 1;
	}

	@Override
	public boolean isLast() {
		return maxPosition != null && currentPosition == maxPosition;
	}

	@Override
	public int getRowNumber() {
		return currentPosition;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		if ( rowNumber == 1 ) {
			return first();
		}
		else if ( rowNumber == -1 ) {
			return last();
		}
		else if ( maxPosition != null && rowNumber == maxPosition ) {
			return last();
		}
		return scroll( rowNumber - currentPosition );
	}
//
//	private boolean isResultSetEmpty() {
//		try {
//			return currentPosition == 0 && !getResultSet().isBeforeFirst() && !getResultSet().isAfterLast();
//		}
//		catch (SQLException e) {
//			throw getSession().getFactory().getSQLExceptionHelper().convert(
//					e,
//					"Could not determine if resultset is empty due to exception calling isBeforeFirst or isAfterLast()"
//			);
//		}
//	}

}
