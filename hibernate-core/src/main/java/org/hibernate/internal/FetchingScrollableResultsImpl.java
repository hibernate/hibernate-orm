/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Implementation of {@link org.hibernate.ScrollableResults} which can handle collection fetches.
 *
 * @author Steve Ebersole
 */
public class FetchingScrollableResultsImpl<R> extends AbstractScrollableResults<R> {
	private R currentRow;

	private int currentPosition;
	private Integer maxPosition;
	private boolean beforeFirst;
	private boolean afterLast;

	public FetchingScrollableResultsImpl(
			JdbcValues jdbcValues,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
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

		maxPosition = jdbcValuesSourceProcessingState.getQueryOptions().getEffectiveLimit().getMaxRows();
		beforeFirst = true;
	}

	@Override
	protected R getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean next() {
		if ( afterLast || isResultSetEmpty() ) {
			return false;
		}
		else if ( maxPosition != null && maxPosition <= currentPosition ) {
			currentPosition = maxPosition + 1;
			currentRow = null;
			afterLast = true;
			beforeFirst = false;
			return false;
		}
		else if ( beforeFirst ) {
			if ( !getRowProcessingState().next() ) {
				// no rows to read
				currentPosition = 0;
				beforeFirst = false;
				return false;
			}
		}

		final boolean last = prepareCurrentRow();

		beforeFirst = false;
		currentPosition++;

		if ( last ) {
			if ( maxPosition == null ) {
				// we just hit the last position
				maxPosition = currentPosition;
			}
		}

		afterScrollOperation();

		return true;
	}


	@Override
	public boolean previous() {
		if ( beforeFirst || isResultSetEmpty() ) {
			return false;
		}
		else if ( currentPosition == 1 ) {
			beforeFirst();
			return false;
		}
		else {
			EntityKey keyToRead = null;
			// This check is needed since processing leaves the cursor
			// after the last physical row for the current logical row;
			// thus if we are after the last physical row, this might be
			// caused by either:
			//      1) scrolling to the last logical row
			//      2) scrolling past the last logical row
			// In the latter scenario, the previous logical row
			// really is the last logical row.
			//
			if ( afterLast ) {
				// position cursor to the last row
				getRowProcessingState().last();
				keyToRead = getEntityKey();
			}
			else {
				// Since the result set cursor is always left at the first
				// physical row after the "last processed", we need to jump
				// back one position to get the key value we are interested
				// in skipping

				getRowProcessingState().previous();

				// sequentially read the result set in reverse until we recognize
				// a change in the key value.  At that point, we are pointed at
				// the last physical sequential row for the logical row in which
				// we are interested in processing
				boolean firstPass = true;
				final EntityKey lastKey = getEntityKey();
				while ( getRowProcessingState().previous() ) {
					final EntityKey checkKey = getEntityKey();
					if ( firstPass ) {
						firstPass = false;
						keyToRead = checkKey;
					}
					if ( !lastKey.equals( checkKey ) ) {
						break;
					}
				}
			}

			// Read backwards until we read past the first physical sequential
			// row with the key we are interested in loading
			while ( getRowProcessingState().previous() ) {
				final EntityKey checkKey = getEntityKey();
				if ( !keyToRead.equals( checkKey ) ) {
					break;
				}
			}

			// Finally, read ahead one row to position result set cursor
			// at the first physical row we are interested in loading
			getRowProcessingState().next();
			prepareCurrentRow();
		}

		afterLast = false;
		currentPosition--;

		afterScrollOperation();

		return true;
	}

	@Override
	public boolean scroll(int positions) {
		boolean more = false;
		if ( positions > 0 ) {
			// scroll ahead
			for ( int i = 0; i < positions; i++ ) {
				more = next();
				if ( !more ) {
					break;
				}
			}
		}
		else if ( positions < 0 ) {
			// scroll backward
			for ( int i = 0; i < -positions; i++ ) {
				more = previous();
				if ( !more ) {
					break;
				}
			}
		}
		else {
			throw new HibernateException( "scroll(0) not valid" );
		}

		afterScrollOperation();

		return more;
	}

	@Override
	public boolean position(int position) {
		return setRowNumber( position );
	}

	@Override
	public boolean last() {
		boolean more = false;
		if ( maxPosition != null ) {
			if ( currentPosition > maxPosition ) {
				more = previous();
			}
			for ( int i = currentPosition; i < maxPosition; i++ ) {
				more = next();
			}
		}
		else {
			if ( isResultSetEmpty() || afterLast ) {
				// should not be able to reach last without maxPosition being set
				// unless there are no results
				return false;
			}

			while ( !afterLast ) {
				more = next();
			}
		}

		afterScrollOperation();

		return more;
	}

	@Override
	public boolean first() {
		beforeFirst();
		final boolean more = next();
//		afterScrollOperation();
		return more;
	}

	@Override
	public void beforeFirst() {
		getRowProcessingState().beforeFirst();
		beforeFirst = true;
		afterLast = false;
		currentRow = null;
		currentPosition = 0;
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
		return currentPosition - 1;
	}

	@Override
	public int getPosition() {
		return currentPosition;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		if ( rowNumber == 1 ) {
			return first();
		}
		else if ( rowNumber == -1 || maxPosition != null && rowNumber == maxPosition ) {
			return last();
		}
		else if ( rowNumber < 0 && maxPosition == null ) {
			while ( next() ) {
				// skip all the way to the end (inefficiently)
			}
			return scroll( rowNumber );
		}
		else {
			// rowNumber -1 is the same as maxPosition
			final int targetRowNumber = rowNumber < 0 ? maxPosition + rowNumber + 1 : rowNumber;
			return scroll( targetRowNumber - currentPosition );
		}
	}

	private boolean prepareCurrentRow() {
		final RowProcessingStateStandardImpl rowProcessingState = getRowProcessingState();
		final RowReader<R> rowReader = getRowReader();

		boolean last = false;
		boolean resultProcessed = false;

		final EntityKey entityKey = getEntityKey();
		final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContext();
		final LoadContexts loadContexts = persistenceContext.getLoadContexts();

		loadContexts.register( getJdbcValuesSourceProcessingState() );
		persistenceContext.beforeLoad();
		try {
			currentRow = rowReader.readRow( rowProcessingState );

			rowProcessingState.finishRowProcessing( true );

			while ( !resultProcessed ) {
				if ( rowProcessingState.next() ) {
					final EntityKey entityKey2 = getEntityKey();
					if ( !entityKey.equals( entityKey2 ) ) {
						resultProcessed = true;
						last = false;
					}
					else {
						rowReader.readRow( rowProcessingState );
						rowProcessingState.finishRowProcessing( false );
					}
				}
				else {
					last = true;
					resultProcessed = true;
				}

			}
			getJdbcValuesSourceProcessingState().finishUp( false );
		}
		finally {
			persistenceContext.afterLoad();
			loadContexts.deregister( getJdbcValuesSourceProcessingState() );
		}
		persistenceContext.initializeNonLazyCollections();
		afterScrollOperation();
		return last;
	}


	private boolean isResultSetEmpty() {
		return currentPosition == 0 && !beforeFirst && !afterLast;
	}

	private EntityKey getEntityKey() {
		return getRowReader().resolveSingleResultEntityKey( getRowProcessingState() );
	}
}
