/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Executor for model-mutation operations
 *
 * @author Steve Ebersole
 */
public interface JdbcMutationExecutor {
	/**
	 * Perform the execution
	 */
	int execute(
			JdbcOperationQueryMutation jdbcMutation,
			JdbcParameterBindings jdbcParameterBindings,
			Function<String, PreparedStatement> statementCreator,
			BiConsumer<Integer, PreparedStatement> expectationCheck,
			ExecutionContext executionContext);
}
