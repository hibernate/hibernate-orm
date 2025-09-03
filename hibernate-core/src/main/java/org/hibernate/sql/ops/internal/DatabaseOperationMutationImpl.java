/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.ops.spi.DatabaseOperationMutation;
import org.hibernate.sql.ops.spi.PostAction;
import org.hibernate.sql.ops.spi.PreAction;

import java.sql.PreparedStatement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Standard DatabaseOperationMutation implementation.
 *
 * @author Steve Ebersole
 */
public class DatabaseOperationMutationImpl
		extends AbstractDatabaseOperation<JdbcOperationQueryMutation>
		implements DatabaseOperationMutation {

	public DatabaseOperationMutationImpl(JdbcOperationQueryMutation primaryOperation) {
		super( primaryOperation );
	}

	public DatabaseOperationMutationImpl(JdbcOperationQueryMutation primaryOperation, PreAction[] preActions, PostAction[] postActions) {
		super( primaryOperation, preActions, postActions );
	}

	@Override
	public int execute(
			Function<String, PreparedStatement> statementCreator,
			JdbcParameterBindings jdbcParameterBindings,
			BiConsumer<Integer, PreparedStatement> expectationCheck,
			ExecutionContext executionContext) {
		final JdbcMutationExecutor jdbcMutationExecutor = executionContext.getSession().getJdbcServices().getJdbcMutationExecutor();
		return jdbcMutationExecutor.execute(
				getPrimaryOperation(),
				jdbcParameterBindings,
				statementCreator,
				expectationCheck,
				executionContext
		);
	}
}
