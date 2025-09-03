/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

import org.hibernate.Incubating;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor.StatementCreator;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import java.sql.PreparedStatement;

/**
 * {@linkplain DatabaseOperation} whose primary operation is a {@linkplain JdbcSelect selection}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DatabaseSelect<S extends JdbcSelect> extends DatabaseOperation<S > {
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
