/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcMutationExecutor implements JdbcMutationExecutor {
	/**
	 * Singleton access
	 */
	public static final StandardJdbcMutationExecutor INSTANCE = new StandardJdbcMutationExecutor();

	@Override
	public int execute(
			JdbcOperationQueryMutation jdbcMutation,
			JdbcParameterBindings jdbcParameterBindings,
			Function<String, PreparedStatement> statementCreator,
			BiConsumer<Integer, PreparedStatement> expectationCheck,
			ExecutionContext executionContext) {
		final var session = executionContext.getSession();
		session.autoFlushIfRequired( jdbcMutation.getAffectedTableNames() );

		final var logicalConnection =
				session.getJdbcCoordinator().getLogicalConnection();

		final var jdbcServices = session.getJdbcServices();
		final String finalSql = applyOptions( jdbcMutation, executionContext, jdbcServices );
		final var queryOptions = executionContext.getQueryOptions();
		try {
			// prepare the query
			final var preparedStatement = statementCreator.apply( finalSql );
			try {
				final Integer timeout = queryOptions.getTimeout();
				if ( timeout != null ) {
					preparedStatement.setQueryTimeout( timeout );
				}

				// bind parameters
				// 		todo : validate that all query parameters were bound?
				int paramBindingPosition = 1;
				for ( var parameterBinder : jdbcMutation.getParameterBinders() ) {
					parameterBinder.bindParameterValue(
							preparedStatement,
							paramBindingPosition++,
							jdbcParameterBindings,
							executionContext
					);
				}

				session.getEventListenerManager().jdbcExecuteStatementStart();
				final var eventMonitor = session.getEventMonitor();
				final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
				try {
					final int rows = preparedStatement.executeUpdate();
					expectationCheck.accept( rows, preparedStatement );
					return rows;
				}
				finally {
					eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, finalSql );
					session.getEventListenerManager().jdbcExecuteStatementEnd();
				}
			}
			finally {
				logicalConnection.getResourceRegistry().release( preparedStatement );
			}
		}
		catch (SQLException e) {
			return handleException( jdbcMutation, e, jdbcServices, finalSql );
		}
		finally {
			executionContext.afterStatement( logicalConnection );
		}
	}

	private static int handleException(
			JdbcOperationQueryMutation jdbcMutation, SQLException sqle, JdbcServices jdbcServices, String finalSql) {
		final var exception =
				jdbcServices.getSqlExceptionHelper()
						.convert( sqle, "JDBC exception executing SQL", finalSql );
		if ( exception instanceof ConstraintViolationException constraintViolationException
				&& jdbcMutation instanceof JdbcOperationQueryInsert jdbcInsert ) {
			if ( constraintViolationException.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE ) {
				final String uniqueConstraintNameThatMayFail = jdbcInsert.getUniqueConstraintNameThatMayFail();
				if ( uniqueConstraintNameThatMayFail != null ) {
					final String violatedConstraintName = constraintViolationException.getConstraintName();
					if ( constraintNameMatches( uniqueConstraintNameThatMayFail, violatedConstraintName ) ) {
						return 0;
					}
				}
			}
		}
		throw exception;
	}

	private static String applyOptions(
			JdbcOperationQueryMutation jdbcMutation, ExecutionContext executionContext, JdbcServices jdbcServices) {
		final var queryOptions = executionContext.getQueryOptions();
		return queryOptions == null
				? jdbcMutation.getSqlString()
				: jdbcServices.getDialect().addSqlHintOrComment(
						jdbcMutation.getSqlString(),
						queryOptions,
						executionContext.getSession().getFactory().getSessionFactoryOptions().isCommentsEnabled()
				);
	}

	private static boolean constraintNameMatches(String uniqueConstraintNameThatMayFail, String violatedConstraintName) {
		return uniqueConstraintNameThatMayFail.isEmpty()
			|| uniqueConstraintNameThatMayFail.equalsIgnoreCase( violatedConstraintName )
			|| violatedConstraintName != null && violatedConstraintName.indexOf('.') > 0
				&& uniqueConstraintNameThatMayFail.equalsIgnoreCase( violatedConstraintName.substring(violatedConstraintName.lastIndexOf('.') + 1) );
	}
}
