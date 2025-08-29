/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

import java.sql.PreparedStatement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@linkplain DatabaseOperation} whose primary operation is a mutation.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DatabaseOperationMutation extends DatabaseOperation {
	/**
	 * Perform the execution.
	 *
	 * @param statementCreator Creator for JDBC {@linkplain PreparedStatement statements}.
	 * @param jdbcParameterBindings Bindings for the JDBC parameters.
	 * @param expectationCheck Check used to verify the outcome of the mutation.
	 * @param executionContext Access to contextual information useful while executing.
	 */
	int execute(
			Function<String, PreparedStatement> statementCreator,
			JdbcParameterBindings jdbcParameterBindings,
			BiConsumer<Integer, PreparedStatement> expectationCheck,
			ExecutionContext executionContext);
}
