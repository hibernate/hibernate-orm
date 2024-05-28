/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.LoadContexts;
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
	}

	@Override
	public void afterLast() {
		getRowProcessingState().afterLast();
	}

	@Override
	public void beforeFirst() {
		getRowProcessingState().beforeFirst();
	}

	@Override
	public boolean isFirst() {
		return getRowProcessingState().isFirst();
	}

	@Override
	public boolean isLast() {
		return getRowProcessingState().isLast();
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

		final PersistenceContext persistenceContext = getPersistenceContext().getPersistenceContext();
		final LoadContexts loadContexts = persistenceContext.getLoadContexts();
		loadContexts.register( getJdbcValuesSourceProcessingState() );
		persistenceContext.beforeLoad();
		try {
			try {
				currentRow = getRowReader().readRow( getRowProcessingState() );

				getRowProcessingState().finishRowProcessing( true );
				getJdbcValuesSourceProcessingState().finishUp( false );
			}
			finally {
				persistenceContext.afterLoad();
			}
			persistenceContext.initializeNonLazyCollections();
		}
		finally {
			loadContexts.deregister( getJdbcValuesSourceProcessingState() );
		}

		afterScrollOperation();
	}

}
