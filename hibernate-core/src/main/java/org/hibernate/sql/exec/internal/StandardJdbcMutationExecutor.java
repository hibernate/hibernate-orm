/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
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
		final SharedSessionContractImplementor session = executionContext.getSession();
		session.autoFlushIfRequired( jdbcMutation.getAffectedTableNames() );

		final LogicalConnectionImplementor logicalConnection = session
				.getJdbcCoordinator()
				.getLogicalConnection();

		final JdbcServices jdbcServices = session.getJdbcServices();
		final QueryOptions queryOptions = executionContext.getQueryOptions();
		final String finalSql;
		if ( queryOptions == null ) {
			finalSql = jdbcMutation.getSqlString();
		}
		else {
			finalSql = jdbcServices.getDialect().addSqlHintOrComment(
					jdbcMutation.getSqlString(),
					queryOptions,
					executionContext.getSession().getFactory().getSessionFactoryOptions().isCommentsEnabled()
			);
		}
		try {
			// prepare the query
			final PreparedStatement preparedStatement = statementCreator.apply( finalSql );

			try {
				if ( executionContext.getQueryOptions().getTimeout() != null ) {
					preparedStatement.setQueryTimeout( executionContext.getQueryOptions().getTimeout() );
				}

				// bind parameters
				// 		todo : validate that all query parameters were bound?
				int paramBindingPosition = 1;
				for ( JdbcParameterBinder parameterBinder : jdbcMutation.getParameterBinders() ) {
					parameterBinder.bindParameterValue(
							preparedStatement,
							paramBindingPosition++,
							jdbcParameterBindings,
							executionContext
					);
				}

				session.getEventListenerManager().jdbcExecuteStatementStart();
				final EventManager eventManager = session.getEventManager();
				final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
				try {
					int rows = preparedStatement.executeUpdate();
					expectationCheck.accept( rows, preparedStatement );
					return rows;
				}
				finally {
					eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, finalSql );
					session.getEventListenerManager().jdbcExecuteStatementEnd();
				}
			}
			finally {
				logicalConnection.getResourceRegistry().release( preparedStatement );
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + finalSql + "]"
			);
		}
		finally {
			executionContext.afterStatement( logicalConnection );
		}
	}
}
