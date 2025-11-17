/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * Consumes {@link JdbcValues} and returns the consumed values in whatever form this
 * consumer returns, generally a {@link java.util.List} or a {@link org.hibernate.ScrollableResults}
 *
 * @author Steve Ebersole
 */
public interface ResultsConsumer<T, R> {
	T consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader);

	boolean canResultsBeCached();
}
