/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;

/**
 * @author Steve Ebersole
 */
public class ListResultsConsumer<R> implements ResultsConsumer<List<R>, R> {
	/**
	 * Singleton access
	 */
	private static final ListResultsConsumer UNIQUE_FILTER_INSTANCE = new ListResultsConsumer(true);
	private static final ListResultsConsumer NORMAL_INSTANCE = new ListResultsConsumer(false);

	@SuppressWarnings("unchecked")
	public static <R> ListResultsConsumer<R> instance(boolean uniqueFilter) {
		return uniqueFilter ? UNIQUE_FILTER_INSTANCE : NORMAL_INSTANCE;
	}

	private final boolean uniqueFilter;

	public ListResultsConsumer(boolean uniqueFilter) {
		this.uniqueFilter = uniqueFilter;
	}

	@Override
	public List<R> consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader) {
		try {
			session.getPersistenceContext().getLoadContexts().register( jdbcValuesSourceProcessingState );

			boolean uniqueRows = false;
			final Class<R> resultJavaType = rowReader.getResultJavaType();
			if ( uniqueFilter && resultJavaType != null && ! resultJavaType.isArray() ) {
				final EntityPersister entityDescriptor = session.getFactory().getMetamodel().findEntityDescriptor( resultJavaType );
				if ( entityDescriptor != null ) {
					uniqueRows = true;
				}
			}

			final List<R> results = new ArrayList<>();

			while ( rowProcessingState.next() ) {
				final R row = rowReader.readRow( rowProcessingState, processingOptions );

				boolean add = true;
				if ( uniqueRows ) {
					if ( results.contains( row ) ) {
						add = false;
					}
				}

				if ( add ) {
					results.add( row );
				}

				rowProcessingState.finishRowProcessing();
			}
			return results;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error processing return rows"
			);
		}
		finally {
			rowReader.finishUp( jdbcValuesSourceProcessingState );
			jdbcValuesSourceProcessingState.finishUp();
			jdbcValues.finishUp();
		}
	}
}
