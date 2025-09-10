/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * Used beneath {@link SelectionQuery#getResultCount()}.
 *
 * @since 6.5
 *
 * @author Gavin King
 */
@Incubating
public class SingleResultConsumer<T> implements ResultsConsumer<T, T> {

	private static final SingleResultConsumer<?> INSTANCE = new SingleResultConsumer<>();

	@SuppressWarnings( "unchecked" )
	public static <T> SingleResultConsumer<T> instance() {
		return (SingleResultConsumer<T>) INSTANCE;
	}

	@Override
	public T consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<T> rowReader) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		RuntimeException ex = null;
		persistenceContext.beforeLoad();
		persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );
		try {
			rowReader.startLoading( rowProcessingState );
			final boolean hadResult = rowProcessingState.next();
			if ( !hadResult ) {
				throw new NoRowException( "SQL query returned no results" );
			}
			final T result = rowReader.readRow( rowProcessingState );
			rowProcessingState.finishRowProcessing( true );
			rowReader.finishUp( rowProcessingState );
			jdbcValuesSourceProcessingState.finishUp( true );
			return result;
		}
		catch (RuntimeException e) {
			ex = e;
		}
		finally {
			try {
				jdbcValues.finishUp( session );
				persistenceContext.afterLoad();
				persistenceContext.getLoadContexts().deregister( jdbcValuesSourceProcessingState );
				persistenceContext.initializeNonLazyCollections();
			}
			catch (RuntimeException e) {
				if ( ex != null ) {
					ex.addSuppressed( e );
				}
				else {
					ex = e;
				}
			}
			finally {
				if ( ex != null ) {
					throw ex;
				}
			}
		}
		throw new IllegalStateException( "Should not reach this" );
	}

	@Override
	public boolean canResultsBeCached() {
		return false;
	}
}
