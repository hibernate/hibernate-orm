/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

/**
 * Base implementation of {@link StatementExecutor}.
 *
 * @author Steve Ebersole
 */
public abstract class BasicExecutor implements StatementExecutor {

	abstract Queryable getPersister();
	abstract String getSql();
	abstract List<ParameterSpecification> getParameterSpecifications();

	@Override
	public String[] getSqlStatements() {
		return new String[] { getSql() };
	}

	@Override
	public int execute(QueryParameters parameters, SharedSessionContractImplementor session)
			throws HibernateException {

		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, getPersister() );
		if ( session.isEventSource() ) {
			( (EventSource) session).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}

		return doExecute(
				session.getJdbcServices().getDialect()
						.addSqlHintOrComment(
								getSql(),
								parameters,
								session.getFactory().getSessionFactoryOptions().isCommentsEnabled()
						),
				parameters,
				getParameterSpecifications(),
				session
		);
	}

	int doExecute(String sql, QueryParameters parameters, List<ParameterSpecification> parameterSpecifications, SharedSessionContractImplementor session)
			throws HibernateException{
		try {
			PreparedStatement st = null;
			try {
				st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				int pos = 1;
				for ( ParameterSpecification parameter: parameterSpecifications) {
					pos += parameter.bind( st, parameters, session, pos );
				}
				RowSelection selection = parameters.getRowSelection();
				if ( selection != null ) {
					if ( selection.getTimeout() != null ) {
						st.setQueryTimeout( selection.getTimeout() );
					}
				}

				return session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st );
			}
			finally {
				if ( st != null ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}
		}
		catch( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper()
					.convert( sqle, "could not execute update query", sql);
		}
	}

}
