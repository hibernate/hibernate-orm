/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;

/**
 * Used beneath {@link SelectionQuery#getResultCount()}.
 *
 * @since 6.5
 *
 * @author Gavin King
 */
@Incubating
public class SingleResultConsumer<T> implements ResultsConsumer<T, T> {
	@Override
	public T consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<T> rowReader) {
		rowReader.startLoading( rowProcessingState );
		rowProcessingState.next();
		final T result = rowReader.readRow( rowProcessingState, processingOptions );
		rowProcessingState.finishRowProcessing( true );
		rowReader.finishUp( jdbcValuesSourceProcessingState );
		jdbcValuesSourceProcessingState.finishUp( false );
		return result;
	}

	@Override
	public boolean canResultsBeCached() {
		return false;
	}
}
