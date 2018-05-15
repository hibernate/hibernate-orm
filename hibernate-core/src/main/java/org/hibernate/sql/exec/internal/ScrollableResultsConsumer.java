/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FetchingScrollableResultsImpl;
import org.hibernate.internal.ScrollableResultsImpl;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.values.JdbcValues;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.RowReader;

/**
 * @author Steve Ebersole
 */
public class ScrollableResultsConsumer<R> implements ResultsConsumer<ScrollableResultsImplementor<R>, R> {
	/**
	 * Singleton access to the standard scrollable-results consumer instance
	 */
	public static final ScrollableResultsConsumer INSTANCE = new ScrollableResultsConsumer();

	@SuppressWarnings("unchecked")
	public static <R> ScrollableResultsConsumer<R> instance() {
		return INSTANCE;
	}

	@Override
	public ScrollableResultsImplementor<R> consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader) {
		if ( containsCollectionFetches( jdbcValues.getResultSetMapping() ) ) {
			return new FetchingScrollableResultsImpl<>(
					jdbcValues,
					processingOptions,
					jdbcValuesSourceProcessingState,
					rowProcessingState,
					rowReader,
					session
			);
		}
		else {
			return new ScrollableResultsImpl<>(
					jdbcValues,
					processingOptions,
					jdbcValuesSourceProcessingState,
					rowProcessingState,
					rowReader,
					session
			);
		}
	}

	private boolean containsCollectionFetches(ResultSetMapping resultSetMapping) {
		return false;
	}
}
