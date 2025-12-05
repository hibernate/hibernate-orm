/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.scrollable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Base implementation of the ScrollableResults interface intended for sharing between
 * {@link ScrollableResultsImpl} and {@link FetchingScrollableResultsImpl}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractScrollableResults<R> implements ScrollableResultsImplementor<R> {
	private final JdbcValues jdbcValues;
	private final JdbcValuesSourceProcessingOptions processingOptions;
	private final JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState;
	private final RowProcessingStateStandardImpl rowProcessingState;
	private final RowReader<R> rowReader;
	private final SharedSessionContractImplementor persistenceContext;

	private boolean closed;

	public AbstractScrollableResults(
			JdbcValues jdbcValues,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			SharedSessionContractImplementor persistenceContext) {
		this.jdbcValues = jdbcValues;
		this.processingOptions = processingOptions;
		this.jdbcValuesSourceProcessingState = jdbcValuesSourceProcessingState;
		this.rowProcessingState = rowProcessingState;
		this.rowReader = rowReader;
		this.persistenceContext = persistenceContext;
	}

	@Override
	public final R get() throws HibernateException {
		if ( closed ) {
			throw new IllegalStateException( "ScrollableResults is closed" );
		}
		return getCurrentRow();
	}

	protected abstract R getCurrentRow();

	protected JdbcValues getJdbcValues() {
		return jdbcValues;
	}

	protected JdbcValuesSourceProcessingOptions getProcessingOptions() {
		return processingOptions;
	}

	protected JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return jdbcValuesSourceProcessingState;
	}

	protected RowProcessingStateStandardImpl getRowProcessingState() {
		return rowProcessingState;
	}

	protected RowReader<R> getRowReader() {
		return rowReader;
	}

	protected SharedSessionContractImplementor getPersistenceContext() {
		return persistenceContext;
	}

	protected void afterScrollOperation() {
		getPersistenceContext().afterScrollOperation();
	}

	@Override
	public void setFetchSize(int fetchSize) {
		getJdbcValues().setFetchSize( fetchSize );
	}

	@Override
	public final void close() {
		if ( !closed ) {
			rowReader.finishUp( rowProcessingState );
			jdbcValues.finishUp( persistenceContext );
			getPersistenceContext().getJdbcCoordinator().afterStatementExecution();
			closed = true;
		}
		// noop if already closed
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
}
