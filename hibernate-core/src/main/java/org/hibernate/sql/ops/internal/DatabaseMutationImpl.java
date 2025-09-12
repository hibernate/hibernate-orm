/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.ops.spi.DatabaseMutation;
import org.hibernate.sql.ops.spi.PostAction;
import org.hibernate.sql.ops.spi.PreAction;

import java.sql.PreparedStatement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Standard {@linkplain DatabaseMutation} implementation.
 *
 * @author Steve Ebersole
 */
public class DatabaseMutationImpl
		extends AbstractDatabaseOperation<JdbcOperationQueryMutation>
		implements DatabaseMutation<JdbcOperationQueryMutation> {

	public DatabaseMutationImpl(JdbcOperationQueryMutation primaryOperation) {
		super( primaryOperation );
	}

	public DatabaseMutationImpl(JdbcOperationQueryMutation primaryOperation, PreAction[] preActions, PostAction[] postActions) {
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
