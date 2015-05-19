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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.type.Type;

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

	private int[] getNamedParameterLocs(String name) throws QueryException {
		final Object loc = customQuery.getNamedParameterBindPoints().get( name );
		if ( loc == null ) {
			throw new QueryException(
					"Named parameter does not appear in Query: " + name,
					customQuery.getSQL() );
		}
		if ( loc instanceof Integer ) {
			return new int[] { (Integer) loc };
		}
		else {
			return ArrayHelper.toIntArray( (List) loc );
		}
	}

	/**
	 * Perform binding of all the JDBC bind parameter values based on the user-defined
	 * positional query parameters (these are the '?'-style hibernate query
	 * params) into the JDBC {@link PreparedStatement}.
	 *
	 * @param st The prepared statement to which to bind the parameter values.
	 * @param queryParameters The query parameters specified by the application.
	 * @param start JDBC paramer binds are positional, so this is the position
	 * from which to start binding.
	 * @param session The session from which the query originated.
	 *
	 * @return The number of JDBC bind positions accounted for during execution.
	 *
	 * @throws SQLException Some form of JDBC error binding the values.
	 * @throws HibernateException Generally indicates a mapping problem or type mismatch.
	 */
	private int bindPositionalParameters(
			final PreparedStatement st,
			final QueryParameters queryParameters,
			final int start,
			final SessionImplementor session) throws SQLException {
		final Object[] values = queryParameters.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters.getFilteredPositionalParameterTypes();
		int span = 0;
		for (int i = 0; i < values.length; i++) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( session.getFactory() );
		}
		return span;
	}

	/**
	 * Perform binding of all the JDBC bind parameter values based on the user-defined
	 * named query parameters into the JDBC {@link PreparedStatement}.
	 *
	 * @param ps The prepared statement to which to bind the parameter values.
	 * @param namedParams The named query parameters specified by the application.
	 * @param start JDBC paramer binds are positional, so this is the position
	 * from which to start binding.
	 * @param session The session from which the query originated.
	 *
	 * @return The number of JDBC bind positions accounted for during execution.
	 *
	 * @throws SQLException Some form of JDBC error binding the values.
	 * @throws HibernateException Generally indicates a mapping problem or type mismatch.
	 */
	private int bindNamedParameters(
			final PreparedStatement ps,
			final Map namedParams,
			final int start,
			final SessionImplementor session) throws SQLException {
		if ( namedParams != null ) {
			// assumes that types are all of span 1
			final Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				final Map.Entry e = (Map.Entry) iter.next();
				final String name = (String) e.getKey();
				final TypedValue typedval = (TypedValue) e.getValue();
				final int[] locs = getNamedParameterLocs( name );
				for ( int loc : locs ) {
					LOG.debugf( "bindNamedParameters() %s -> %s [%s]", typedval.getValue(), name, loc + start );
					typedval.getType().nullSafeSet(
							ps,
							typedval.getValue(),
							loc + start,
							session
					);
				}
				result += locs.length;
			}
			return result;
		}

		return 0;
	}

	protected void coordinateSharedCacheCleanup(SessionImplementor session) {
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
			SessionImplementor session) throws HibernateException {

		coordinateSharedCacheCleanup( session );

		if ( queryParameters.isCallable() ) {
			throw new IllegalArgumentException("callable not yet supported for native queries");
		}

		int result = 0;
		PreparedStatement ps;
		try {
			queryParameters.processFilters( this.customQuery.getSQL(), session );
			final String sql = queryParameters.getFilteredSQL();

			ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );

			try {
				int col = 1;
				col += bindPositionalParameters( ps, queryParameters, col, session );
				col += bindNamedParameters( ps, queryParameters.getNamedParameters(), col, session );
				result = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			finally {
				if ( ps != null ) {
					session.getJdbcCoordinator().getResourceRegistry().release( ps );
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
