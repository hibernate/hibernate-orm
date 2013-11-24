/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.engine.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Centralizes the commonality regarding binding of parameter values into PreparedStatements as this logic is
 * used in many places.
 * <p/>
 * Ideally would like to move to the parameter handling as it is done in the hql.ast package.
 *
 * @author Steve Ebersole
 */
public class ParameterBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ParameterBinder.class.getName()
	);

	/**
	 * Helper contract for dealing with named parameters and resolving their locations
	 */
	public static interface NamedParameterSource {
		/**
		 * Retrieve the locations for the given parameter name
		 *
		 * @param name The parameter name
		 *
		 * @return The locations
		 */
		public int[] getNamedParameterLocations(String name);
	}

	private ParameterBinder() {
	}

	/**
	 * Perform parameter binding
	 *
	 * @param st The statement to bind parameters to
	 * @param queryParameters The parameters
	 * @param start The initial bind position
	 * @param source The named parameter source, for resolving the locations of named parameters
	 * @param session The session
	 *
	 * @return The next bind position after the last position we bound here.
	 *
	 * @throws SQLException Indicates a problem calling JDBC bind methods
	 * @throws HibernateException Indicates a problem access bind values.
	 */
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

	private static int bindPositionalParameters(
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

	private static int bindPositionalParameters(
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

	private static int bindNamedParameters(
			final PreparedStatement ps,
			final QueryParameters queryParameters,
			final int start,
			final NamedParameterSource source,
			final SessionImplementor session) throws SQLException, HibernateException {
		return bindNamedParameters( ps, queryParameters.getNamedParameters(), start, source, session );
	}

	private static int bindNamedParameters(
			final PreparedStatement ps,
			final Map namedParams,
			final int start,
			final NamedParameterSource source,
			final SessionImplementor session) throws SQLException, HibernateException {
		if ( namedParams != null ) {
			final boolean debugEnabled = LOG.isDebugEnabled();
			// assumes that types are all of span 1
			final Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				final Map.Entry e = (Map.Entry) iter.next();
				final String name = (String) e.getKey();
				final TypedValue typedVal = (TypedValue) e.getValue();
				final int[] locations = source.getNamedParameterLocations( name );
				for ( int location : locations ) {
					if ( debugEnabled ) {
						LOG.debugf(
								"bindNamedParameters() %s -> %s [%s]",
								typedVal.getValue(),
								name,
								location + start
						);
					}
					typedVal.getType().nullSafeSet( ps, typedVal.getValue(), location + start, session );
				}
				result += locations.length;
			}
			return result;
		}
		return 0;
	}
}
