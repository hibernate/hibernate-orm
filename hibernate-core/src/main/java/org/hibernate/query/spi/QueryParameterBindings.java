/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.query.NamedQueryParameter;
import org.hibernate.query.PositionalQueryParameter;
import org.hibernate.query.internal.QueryParameterBindingImpl;

/**
 * Manages the group of QueryParameterBinding for a particular query.
 *
 * @author Steve Ebersole
 */
@Incubating
public class QueryParameterBindings {
	private Map<QueryParameter, QueryParameterBinding> parameterBindingMap;

	public QueryParameterBindings() {
		this( Collections.<QueryParameter>emptySet() );
	}

	public QueryParameterBindings(Set<QueryParameter> queryParameters) {
		if ( queryParameters == null || queryParameters.isEmpty() ) {
			parameterBindingMap = Collections.emptyMap();
		}
		else {
			parameterBindingMap = new HashMap<QueryParameter, QueryParameterBinding>();

			for ( QueryParameter queryParameter : queryParameters ) {
				parameterBindingMap.put( queryParameter, new QueryParameterBindingImpl() );
			}
		}
	}

	public QueryParameterBinding getBinding(QueryParameter parameter) {
		return parameterBindingMap.get( parameter );
	}

	public QueryParameterBinding getNamedParameterBinding(String name) {
		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			if ( entry.getKey() instanceof NamedQueryParameter ) {
				if ( name.equals( ( (NamedQueryParameter) entry.getKey() ).getName() ) ) {
					return entry.getValue();
				}
			}
		}

		throw new UnknownParameterException( "Unknown named parameter : " + name );
	}

	public QueryParameterBinding getPositionalParameterBinding(Integer position) {
		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			if ( entry.getKey() instanceof PositionalQueryParameter ) {
				if ( position.equals( ( (PositionalQueryParameter) entry.getKey() ).getPosition() ) ) {
					return entry.getValue();
				}
			}
		}

		throw new UnknownParameterException( "Unknown positional parameter : " + position );
	}

	public QueryParameterBinding getParameterBinding(QueryParameter parameter) {
		// see if this exact instance is known as a key
		if ( parameterBindingMap.containsKey( parameter ) ) {
			return parameterBindingMap.get( parameter );
		}

		if ( parameter instanceof NamedQueryParameter ) {
			return getNamedParameterBinding( ( (NamedQueryParameter) parameter ).getName() );
		}
		if ( parameter instanceof PositionalQueryParameter ) {
			return getPositionalParameterBinding( ( (PositionalQueryParameter) parameter ).getPosition() );
		}

		throw new UnknownParameterException( "Could not resolve  parameter [" + parameter + "] as part of query" );
	}
}
