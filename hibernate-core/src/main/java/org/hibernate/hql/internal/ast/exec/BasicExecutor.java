/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

import antlr.RecognitionException;

/**
 * Implementation of BasicExecutor.
 *
 * @author Steve Ebersole
 */
public class BasicExecutor implements StatementExecutor {
	private final Queryable persister;
	private final String sql;
	private final List parameterSpecifications;

	public BasicExecutor(HqlSqlWalker walker, Queryable persister) {
		this.persister = persister;
		try {
			SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
			gen.statement( walker.getAST() );
			sql = gen.getSQL();
			gen.getParseErrorHandler().throwQueryException();
			parameterSpecifications = gen.getCollectedParameters();
		}
		catch ( RecognitionException e ) {
			throw QuerySyntaxException.convert( e );
		}
	}

	@Override
	public String[] getSqlStatements() {
		return new String[] { sql };
	}

	@Override
	public int execute(QueryParameters parameters, SharedSessionContractImplementor session) throws HibernateException {
		return doExecute(
			parameters,
			session,
			session.getJdbcServices().getDialect()
					.addSqlHintOrComment(
						sql,
						parameters,
						session.getFactory().getSessionFactoryOptions().isCommentsEnabled()
					),
			parameterSpecifications
		);
	}
	
	protected int doExecute(QueryParameters parameters, SharedSessionContractImplementor session, String sql,
			List parameterSpecifications) throws HibernateException {
		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, persister );
		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}

		PreparedStatement st = null;
		RowSelection selection = parameters.getRowSelection();

		try {
			try {
				st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				Iterator paramSpecItr = parameterSpecifications.iterator();
				int pos = 1;
				while ( paramSpecItr.hasNext() ) {
					final ParameterSpecification paramSpec = (ParameterSpecification) paramSpecItr.next();
					pos += paramSpec.bind( st, parameters, session, pos );
				}
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
			throw session.getJdbcServices().getSqlExceptionHelper().convert( sqle, "could not execute update query", sql );
		}
	}
}
