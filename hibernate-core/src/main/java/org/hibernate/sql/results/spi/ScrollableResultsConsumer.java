/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FetchingScrollableResultsImpl;
import org.hibernate.internal.ScrollableResultsImpl;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;

/**
 * @author Steve Ebersole
 */
public class ScrollableResultsConsumer<R> implements ResultsConsumer<ScrollableResultsImplementor<R>, R> {
	/**
	 * Singleton access to the standard scrollable-results consumer instance
	 *
	 * @deprecated in favor of {@link #instance()}
	 */
	@SuppressWarnings( "rawtypes" )
	@Deprecated( forRemoval = true )
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
		rowReader.startLoading( rowProcessingState );
		if ( containsCollectionFetches( jdbcValues.getValuesMapping() ) ) {
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

	@Override
	public boolean canResultsBeCached() {
		return false;
	}

	private boolean containsCollectionFetches(JdbcValuesMapping valuesMapping) {
		final List<DomainResult<?>> domainResults = valuesMapping.getDomainResults();
		for ( DomainResult<?> domainResult : domainResults ) {
			if ( domainResult instanceof EntityResult entityResult && entityResult.containsCollectionFetches() ) {
				return true;
			}
		}
		return false;
	}
}
