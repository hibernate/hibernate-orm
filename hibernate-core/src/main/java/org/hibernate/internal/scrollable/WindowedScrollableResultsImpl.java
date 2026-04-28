/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.scrollable;

import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * Scrollable results view applying an in-memory offset/limit without
 * materializing all rows in a list up front.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public class WindowedScrollableResultsImpl<R> extends AbstractScrollableResults<R> {
	private final ScrollableResultsImplementor<R> delegate;
	private final int firstRow;
	private final Integer maxRows;

	private R currentRow;
	private int currentPosition;
	private Integer resultSize;
	private boolean beforeFirst = true;
	private boolean afterLast;
	private boolean delegateBeforeFirst = true;

	public WindowedScrollableResultsImpl(AbstractScrollableResults<R> delegate, int firstRow, Integer maxRows) {
		super(
				delegate.getJdbcValues(),
				delegate.getProcessingOptions(),
				delegate.getJdbcValuesSourceProcessingState(),
				delegate.getRowProcessingState(),
				delegate.getRowReader(),
				delegate.getPersistenceContext()
		);
		this.delegate = delegate;
		this.firstRow = Math.max( firstRow, 0 );
		this.maxRows = maxRows;
	}

	@Override
	protected R getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean next() {
		if ( afterLast ) {
			return false;
		}
		if ( beforeFirst ) {
			return moveToPosition( 1 );
		}
		if ( maxRows != null && currentPosition >= maxRows ) {
			resultSize = currentPosition;
			setAfterLastState();
			return false;
		}

		final boolean hasResult = delegate.next();
		if ( hasResult ) {
			currentPosition++;
			currentRow = delegate.get();
			beforeFirst = false;
			afterLast = false;
			delegateBeforeFirst = false;
		}
		else {
			resultSize = currentPosition;
			setAfterLastState();
		}
		return hasResult;
	}

	@Override
	public boolean previous() {
		if ( beforeFirst ) {
			return false;
		}
		if ( afterLast ) {
			ensureResultSize();
			return resultSize != null && resultSize > 0 && moveToPosition( resultSize );
		}
		if ( currentPosition == 1 ) {
			beforeFirst();
			return false;
		}

		final boolean hasResult = delegate.previous();
		if ( hasResult ) {
			currentPosition--;
			currentRow = delegate.get();
			beforeFirst = false;
			afterLast = false;
			delegateBeforeFirst = false;
			return true;
		}

		beforeFirst();
		return false;
	}

	@Override
	public boolean scroll(int positions) {
		if ( positions == 0 ) {
			return currentPosition > 0;
		}

		boolean hasResult = false;
		if ( positions > 0 ) {
			for ( int i = 0; i < positions; i++ ) {
				hasResult = next();
				if ( !hasResult ) {
					break;
				}
			}
		}
		else {
			for ( int i = 0; i < -positions; i++ ) {
				hasResult = previous();
				if ( !hasResult ) {
					break;
				}
			}
		}
		return hasResult;
	}

	@Override
	public boolean position(int position) {
		if ( position > 0 ) {
			return moveToPosition( position );
		}
		if ( position < 0 ) {
			ensureResultSize();
			final int target = resultSize + position + 1;
			if ( target > 0 ) {
				return moveToPosition( target );
			}
		}
		beforeFirst();
		return false;
	}

	@Override
	public int getPosition() {
		return currentPosition;
	}

	@Override
	public boolean last() {
		ensureResultSize();
		return resultSize != null && resultSize > 0 && moveToPosition( resultSize );
	}

	@Override
	public boolean first() {
		return moveToPosition( 1 );
	}

	@Override
	public void beforeFirst() {
		delegate.beforeFirst();
		delegateBeforeFirst = true;
		currentRow = null;
		currentPosition = 0;
		beforeFirst = true;
		afterLast = false;
	}

	@Override
	public void afterLast() {
		ensureResultSize();
		setAfterLastState();
	}

	@Override
	public boolean isFirst() {
		return currentPosition == 1;
	}

	@Override
	public boolean isLast() {
		if ( currentPosition == 0 ) {
			return false;
		}
		ensureResultSize();
		return currentPosition == resultSize;
	}

	@Override
	public int getRowNumber() {
		return currentPosition > 0 ? currentPosition - 1 : -1;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		return position( rowNumber );
	}

	private boolean moveToPosition(int targetPosition) {
		if ( targetPosition < 1 ) {
			beforeFirst();
			return false;
		}
		if ( maxRows != null && targetPosition > maxRows ) {
			ensureResultSize();
			if ( targetPosition > resultSize ) {
				setAfterLastState();
				return false;
			}
		}
		if ( resultSize != null && targetPosition > resultSize ) {
			setAfterLastState();
			return false;
		}

		if ( !delegateBeforeFirst ) {
			delegate.beforeFirst();
			delegateBeforeFirst = true;
		}

		for ( int skipped = 0; skipped < firstRow; skipped++ ) {
			if ( !delegate.next() ) {
				resultSize = 0;
				setAfterLastState();
				return false;
			}
			delegateBeforeFirst = false;
		}

		for ( int position = 1; position <= targetPosition; position++ ) {
			if ( !delegate.next() ) {
				resultSize = position - 1;
				setAfterLastState();
				return false;
			}
			delegateBeforeFirst = false;
		}

		currentPosition = targetPosition;
		currentRow = delegate.get();
		beforeFirst = false;
		afterLast = false;
		return true;
	}

	private void ensureResultSize() {
		if ( resultSize == null ) {
			final int restorePosition = currentPosition;
			final boolean restoreBeforeFirst = beforeFirst;
			final boolean restoreAfterLast = afterLast;

			beforeFirst();

			int size = 0;
			while ( next() ) {
				size = currentPosition;
			}
			resultSize = size;

			if ( restoreBeforeFirst ) {
				beforeFirst();
			}
			else if ( restoreAfterLast ) {
				setAfterLastState();
			}
			else {
				moveToPosition( restorePosition );
			}
		}
	}

	private void setAfterLastState() {
		currentRow = null;
		currentPosition = 0;
		beforeFirst = false;
		afterLast = true;
		delegateBeforeFirst = false;
	}
}
