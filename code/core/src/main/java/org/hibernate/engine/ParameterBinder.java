// $Id: ParameterBinder.java 7385 2005-07-06 17:13:15Z steveebersole $
package org.hibernate.engine;

import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Iterator;

/**
 * Centralizes the commonality regarding binding of parameter values into
 * PreparedStatements as this logic is used in many places.
 * <p/>
 * Ideally would like to move to the parameter handling as it is done in
 * the hql.ast package.
 * 
 * @author Steve Ebersole
 */
public class ParameterBinder {

	private static final Log log = LogFactory.getLog( ParameterBinder.class );

	public static interface NamedParameterSource {
		public int[] getNamedParameterLocations(String name);
	}

	private ParameterBinder() {
	}

	public static int bindQueryParameters(
	        final PreparedStatement st,
	        final QueryParameters queryParameters,
	        final int start,
	        final NamedParameterSource source,
	        SessionImplementor session) throws SQLException, HibernateException {
		int col = start;
		col += bindPositionalParameters( st, queryParameters, col, session );
		col += bindNamedParameters( st, queryParameters, col, source, session );
		return col;
	}

	public static int bindPositionalParameters(
	        final PreparedStatement st,
	        final QueryParameters queryParameters,
	        final int start,
	        final SessionImplementor session) throws SQLException, HibernateException {
		return bindPositionalParameters(
		        st,
		        queryParameters.getPositionalParameterValues(),
		        queryParameters.getPositionalParameterTypes(),
		        start,
		        session
		);
	}

	public static int bindPositionalParameters(
	        final PreparedStatement st,
	        final Object[] values,
	        final Type[] types,
	        final int start,
	        final SessionImplementor session) throws SQLException, HibernateException {
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( session.getFactory() );
		}
		return span;
	}

	public static int bindNamedParameters(
	        final PreparedStatement ps,
	        final QueryParameters queryParameters,
	        final int start,
	        final NamedParameterSource source,
	        final SessionImplementor session) throws SQLException, HibernateException {
		return bindNamedParameters( ps, queryParameters.getNamedParameters(), start, source, session );
	}

	public static int bindNamedParameters(
	        final PreparedStatement ps,
	        final Map namedParams,
	        final int start,
	        final NamedParameterSource source,
	        final SessionImplementor session) throws SQLException, HibernateException {
		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = ( Map.Entry ) iter.next();
				String name = ( String ) e.getKey();
				TypedValue typedval = ( TypedValue ) e.getValue();
				int[] locations = source.getNamedParameterLocations( name );
				for ( int i = 0; i < locations.length; i++ ) {
					if ( log.isDebugEnabled() ) {
						log.debug( "bindNamedParameters() " +
								typedval.getValue() + " -> " + name +
								" [" + ( locations[i] + start ) + "]" );
					}
					typedval.getType().nullSafeSet( ps, typedval.getValue(), locations[i] + start, session );
				}
				result += locations.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}
}
