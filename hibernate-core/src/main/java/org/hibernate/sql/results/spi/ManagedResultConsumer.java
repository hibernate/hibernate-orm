/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;

/**
 * Reads rows without producing a result.
 *
 * @since 6.6
 */
@Incubating
public class ManagedResultConsumer implements ResultsConsumer<Void, Object> {

	public static final ManagedResultConsumer INSTANCE = new ManagedResultConsumer();

	@Override
	public Void consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<Object> rowReader) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		RuntimeException ex = null;
		persistenceContext.beforeLoad();
		persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );
		try {
			rowReader.startLoading( rowProcessingState );
			while ( rowProcessingState.next() ) {
				rowReader.readRow( rowProcessingState );
				rowProcessingState.finishRowProcessing( true );
			}
			rowReader.finishUp( rowProcessingState );
			jdbcValuesSourceProcessingState.finishUp( true );
		return null;
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
