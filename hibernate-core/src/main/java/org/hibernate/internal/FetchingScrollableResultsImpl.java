/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Implementation of ScrollableResults which can handle collection fetches.
 *
 * @author Steve Ebersole
 */
public class FetchingScrollableResultsImpl<R> extends AbstractScrollableResults<R> {
	private final EntityInitializer resultInitializer;

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

		resultInitializer = extractResultInitializer( rowReader );

		this.maxPosition = jdbcValuesSourceProcessingState.getQueryOptions().getEffectiveLimit().getMaxRows();
	}

	private static <R> EntityInitializer extractResultInitializer(RowReader<R> rowReader) {
		Initializer initializer = rowReader.getInitializers().get( rowReader.getInitializers().size() - 1 );
		return initializer.asEntityInitializer(); //might return null when it's not an EntityInitializer (intentional)
	}

	@Override
	protected R getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean next() {
		if ( maxPosition != null && maxPosition <= currentPosition ) {
			currentRow = null;
			currentPosition = maxPosition + 1;
			return false;
		}

		if ( isResultSetEmpty() ) {
			currentRow = null;
			currentPosition = 0;
			return false;
		}

		boolean afterLast = prepareCurrentRow();

		currentPosition++;

		if ( afterLast ) {
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
		if ( currentPosition <= 1 ) {
			currentPosition = 0;
			currentRow = null;
			return false;
		}

		if ( getRowProcessingState().isFirst() ) {
			// don't even bother trying to read any further
			currentRow = null;
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
			if ( getRowProcessingState().isAfterLast() && maxPosition != null && currentPosition > maxPosition ) {
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
					EntityKey checkKey = getEntityKey();

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
				EntityKey checkKey = getEntityKey();

				if ( !keyToRead.equals( checkKey ) ) {
					break;
				}
			}

			// Finally, read ahead one row to position result set cursor
			// at the first physical row we are interested in loading
			getRowProcessingState().next();
			prepareCurrentRow();
		}

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
		final boolean underlyingScrollSuccessful = getRowProcessingState().position( position );
		if ( !underlyingScrollSuccessful ) {
			currentRow = null;
			return false;

		}
		currentPosition = position - 1;
		return next();
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
			final RowProcessingStateStandardImpl rowProcessingState = getRowProcessingState();
			if ( isResultSetEmpty() || rowProcessingState.isAfterLast() ) {
				// should not be able to reach last without maxPosition being set
				// unless there are no results
				return false;
			}

			while ( !rowProcessingState.isAfterLast() ) {
				more = next();
			}
		}

		afterScrollOperation();

		return more;
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
		getRowProcessingState().beforeFirst();
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

	private boolean prepareCurrentRow() {
		if ( getRowProcessingState().isBeforeFirst() ) {
			getRowProcessingState().next();
		}

		final RowReader<R> rowReader = getRowReader();

		boolean afterLast = false;
		boolean resultProcessed = false;

		final EntityKey entityKey = getEntityKey();

		currentRow = rowReader.readRow( getRowProcessingState(), getProcessingOptions() );

		getRowProcessingState().finishRowProcessing();

		while ( !resultProcessed ) {
			if ( getRowProcessingState().next() ) {
				final EntityKey entityKey2 = getEntityKey();
				if ( !entityKey.equals( entityKey2 ) ) {
					resultInitializer.finishUpRow( getRowProcessingState() );
					resultProcessed = true;
					afterLast = false;
				}
				else {
					rowReader.readRow( getRowProcessingState(), getProcessingOptions() );
					getRowProcessingState().finishRowProcessing();
				}
			}
			else {
				afterLast = true;
				resultProcessed = true;
			}

		}
		getJdbcValuesSourceProcessingState().finishUp();
		getRowProcessingState().getSession().getPersistenceContext().initializeNonLazyCollections();
		return afterLast;
	}


	private boolean isResultSetEmpty() {
		return currentPosition == 0 && !getRowProcessingState().isBeforeFirst() && !getRowProcessingState().isAfterLast();
	}

	private EntityKey getEntityKey() {
		resultInitializer.resolveKey( getRowProcessingState() );
		final EntityKey entityKey = resultInitializer.getEntityKey();
		resultInitializer.finishUpRow( getRowProcessingState() );
		return entityKey;
	}

}
