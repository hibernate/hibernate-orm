/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Consumes {@link JdbcValues} and returns the consumed values in whatever form this
 * consumer returns, generally a {@link java.util.List} or a {@link org.hibernate.ScrollableResults}
 *
 * @see org.hibernate.sql.exec.spi.JdbcSelectExecutor#executeQuery(org.hibernate.sql.exec.spi.JdbcSelect, org.hibernate.sql.exec.spi.JdbcParameterBindings, org.hibernate.sql.exec.spi.ExecutionContext, RowTransformer, Class, org.hibernate.sql.exec.spi.StatementCreator, ResultsConsumer)
 * @see org.hibernate.query.spi.SelectQueryPlan#executeQuery(org.hibernate.query.spi.DomainQueryExecutionContext, ResultsConsumer)
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultsConsumer<T, R> {
	/// Consumes one JDBC result stream using only supported processing contexts.
	/// Do not retain the row-processing state after this callback returns.
	T consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingState rowProcessingState,
			RowReader<R> rowReader);

	boolean canResultsBeCached();
}
