/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.sql.exec.spi.JdbcSelectExecutor.StatementCreator;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import java.sql.PreparedStatement;

/**
 * {@linkplain DatabaseOperation} whose primary operation is a selection.
 *
 * @author Steve Ebersole
 */
public interface DatabaseOperationSelect extends DatabaseOperation {
	@Override
	JdbcOperationQuerySelect getPrimaryOperation();

	/**
	 * Execute the underlying statements and return the result(s).
	 *
	 * @param resultType The expected type of domain result values.
	 * @param expectedNumberOfRows The number of domain results expected.
	 * @param statementCreator Creator for JDBC {@linkplain PreparedStatement statements}.
	 * @param jdbcParameterBindings Bindings for the JDBC parameters.
	 * @param rowTransformer Any row transformation to apply.
	 * @param resultsConsumer Consumer for each domain result.
	 * @param executionContext Access to contextual information useful while executing.
	 *
	 * @return The indicated result(s).
	 */
	<T,R> T execute(
			Class<R> resultType,
			int expectedNumberOfRows,
			StatementCreator statementCreator,
			JdbcParameterBindings jdbcParameterBindings,
			RowTransformer<R> rowTransformer,
			ResultsConsumer<T, R> resultsConsumer,
			ExecutionContext executionContext);
}
