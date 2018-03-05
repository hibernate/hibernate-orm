/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.param.ParameterBinder;

/**
 * Defines a query execution plan for a native-SQL query.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryPlan implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( NativeSQLQueryPlan.class );

	private final String sourceQuery;
	private final CustomQuery customQuery;

	/**
	  * Constructs a NativeSQLQueryPlan.
	  *
	  * @param sourceQuery The original native query to create a plan for
	  * @param customQuery The query executed via this plan
	  */
	public NativeSQLQueryPlan(String sourceQuery, CustomQuery customQuery) {
		this.sourceQuery = sourceQuery;
		this.customQuery = customQuery;
	}

	public String getSourceQuery() {
		return sourceQuery;
	}

	public CustomQuery getCustomQuery() {
		return customQuery;
	}

	protected void coordinateSharedCacheCleanup(SharedSessionContractImplementor session) {
		final BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, getCustomQuery().getQuerySpaces() );

		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}

	/**
	 * Performs the execute query
	 *
	 * @param queryParameters The query parameters
	 * @param session The session
	 *
	 * @return The number of affected rows as returned by the JDBC driver
	 *
	 * @throws HibernateException Indicates a problem performing the query execution
	 */
	public int performExecuteUpdate(
			QueryParameters queryParameters,
			SharedSessionContractImplementor session) throws HibernateException {

		coordinateSharedCacheCleanup( session );

		if ( queryParameters.isCallable() ) {
			throw new IllegalArgumentException("callable not yet supported for native queries");
		}

		int result = 0;
		PreparedStatement ps;
		RowSelection selection = queryParameters.getRowSelection();
		try {
			queryParameters.processFilters( this.customQuery.getSQL(), session );
			final String sql = session.getJdbcServices().getDialect()
					.addSqlHintOrComment(
						queryParameters.getFilteredSQL(),
						queryParameters,
						session.getFactory().getSessionFactoryOptions().isCommentsEnabled()
					);

			ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );

			try {
				int col = 1;
				for ( ParameterBinder binder : this.customQuery.getParameterValueBinders() ) {
					col += binder.bind( ps, queryParameters, session, col );
				}
				if ( selection != null && selection.getTimeout() != null ) {
					ps.setQueryTimeout( selection.getTimeout() );
				}
				result = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			finally {
				if ( ps != null ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}
		}
		catch (SQLException sqle) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not execute native bulk manipulation query",
					this.sourceQuery
			);
		}

		return result;
	}
}
