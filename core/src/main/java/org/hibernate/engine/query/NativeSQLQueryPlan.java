package org.hibernate.engine.query;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.action.BulkOperationCleanupAction;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.event.EventSource;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.loader.custom.sql.SQLCustomQuery;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * Defines a query execution plan for a native-SQL query.
 * 
 * @author Steve Ebersole
 */
public class NativeSQLQueryPlan implements Serializable {
	private final String sourceQuery;

	private final SQLCustomQuery customQuery;

	private static final Log log = LogFactory.getLog(NativeSQLQueryPlan.class);

	public NativeSQLQueryPlan(
			NativeSQLQuerySpecification specification,
			SessionFactoryImplementor factory) {
		this.sourceQuery = specification.getQueryString();

		customQuery = new SQLCustomQuery(
				specification.getQueryString(),
				specification.getQueryReturns(),
				specification.getQuerySpaces(),
				factory );
	}

	public String getSourceQuery() {
		return sourceQuery;
	}

	public SQLCustomQuery getCustomQuery() {
		return customQuery;
	}

	private int[] getNamedParameterLocs(String name) throws QueryException {
		Object loc = customQuery.getNamedParameterBindPoints().get( name );
		if ( loc == null ) {
			throw new QueryException(
					"Named parameter does not appear in Query: " + name,
					customQuery.getSQL() );
		}
		if ( loc instanceof Integer ) {
			return new int[] { ((Integer) loc ).intValue() };
		}
		else {
			return ArrayHelper.toIntArray( (List) loc );
		}
	}

	/**
	 * Bind positional parameter values to the <tt>PreparedStatement</tt>
	 * (these are parameters specified by a JDBC-style ?).
	 */
	private int bindPositionalParameters(final PreparedStatement st,
			final QueryParameters queryParameters, final int start,
			final SessionImplementor session) throws SQLException,
			HibernateException {

		final Object[] values = queryParameters
				.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters
				.getFilteredPositionalParameterTypes();
		int span = 0;
		for (int i = 0; i < values.length; i++) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( session.getFactory() );
		}
		return span;
	}

	/**
	 * Bind named parameters to the <tt>PreparedStatement</tt>. This has an
	 * empty implementation on this superclass and should be implemented by
	 * subclasses (queries) which allow named parameters.
	 */
	private int bindNamedParameters(final PreparedStatement ps,
			final Map namedParams, final int start,
			final SessionImplementor session) throws SQLException,
			HibernateException {

		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = (Map.Entry) iter.next();
				String name = (String) e.getKey();
				TypedValue typedval = (TypedValue) e.getValue();
				int[] locs = getNamedParameterLocs( name );
				for (int i = 0; i < locs.length; i++) {
					if ( log.isDebugEnabled() ) {
						log.debug( "bindNamedParameters() "
								+ typedval.getValue() + " -> " + name + " ["
								+ (locs[i] + start ) + "]" );
					}
					typedval.getType().nullSafeSet( ps, typedval.getValue(),
							locs[i] + start, session );
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}

	protected void coordinateSharedCacheCleanup(SessionImplementor session) {
		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, getCustomQuery().getQuerySpaces() );

		action.init();

		if ( session.isEventSource() ) {
			( ( EventSource ) session ).getActionQueue().addAction( action );
		}
	}

	public int performExecuteUpdate(QueryParameters queryParameters,
			SessionImplementor session) throws HibernateException {
		
		coordinateSharedCacheCleanup( session );
		
		if(queryParameters.isCallable()) {
			throw new IllegalArgumentException("callable not yet supported for native queries");
		}
		
		int result = 0;
		PreparedStatement ps;
		try {
			queryParameters.processFilters( this.customQuery.getSQL(),
					session );
			String sql = queryParameters.getFilteredSQL();

			ps = session.getBatcher().prepareStatement( sql );

			try {
				int col = 1;
				col += bindPositionalParameters( ps, queryParameters, col,
						session );
				col += bindNamedParameters( ps, queryParameters
						.getNamedParameters(), col, session );
				result = ps.executeUpdate();
			}
			finally {
				if ( ps != null ) {
					session.getBatcher().closeStatement( ps );
				}				
			}			
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert( session.getFactory()
					.getSQLExceptionConverter(), sqle,
					"could not execute native bulk manipulation query", this.sourceQuery );
		}

		return result;
	}
	
}
