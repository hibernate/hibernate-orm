/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.engine;

import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger( ParameterBinder.class );

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
