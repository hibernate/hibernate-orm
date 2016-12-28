/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.persistence.Parameter;

import org.hibernate.QueryParameterException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;

/**
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadataImplementor {
	private final Map<String,QueryParameter> namedQueryParameters;
	private final Map<Integer,QueryParameter> positionalQueryParameters;

	public ParameterMetadataImpl(
			Map<String, QueryParameter> namedQueryParameters,
			Map<Integer, QueryParameter> positionalQueryParameters) {
		this.namedQueryParameters = namedQueryParameters != null ? namedQueryParameters : Collections.emptyMap();
		this.positionalQueryParameters = positionalQueryParameters != null ? positionalQueryParameters : Collections.emptyMap();
	}

	public Map<String, QueryParameter> namedQueryParameters() {
		return namedQueryParameters;
	}

	public Map<Integer, QueryParameter> positionalQueryParameters() {
		return positionalQueryParameters;
	}

	@Override
	public boolean hasNamedParameters() {
		return !namedQueryParameters.isEmpty();
	}

	@Override
	public boolean hasPositionalParameters() {
		return !positionalQueryParameters.isEmpty();
	}

	@Override
	public int getNamedParameterCount() {
		return namedQueryParameters.size();
	}

	@Override
	public int getPositionalParameterCount() {
		return positionalQueryParameters.size();
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return  namedQueryParameters.keySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getQueryParameter(String name) {
		final QueryParameter parameter = namedQueryParameters.get( name );
		if ( parameter == null ) {
			throw new QueryParameterException( "could not locate named parameter [" + name + "]" );
		}
		return parameter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getQueryParameter(int position) {
		if ( !hasPositionalParameters() ) {
			throw new QueryParameterException( "Query did not define positional parameters" );
		}

		if ( position < 1 || position > positionalQueryParameters.size() ) {
			throw new QueryParameterException(
					"Invalid parameter position [" + position + "]; should be between 1 and " + positionalQueryParameters.size()
			);
		}

		return positionalQueryParameters.get( position );
	}

	@Override
	public <T> QueryParameter<T> resolve(Parameter<T> param) {
		if ( param instanceof QueryParameter ) {
			return (QueryParameter<T>) param;
		}

		if ( param.getName() != null ) {
			return getQueryParameter( param.getName() );
		}

		if ( param.getPosition() != null ) {
			return getQueryParameter( param.getPosition() );
		}

		throw new IllegalArgumentException( "Could not resolve javax.persistence.Parameter to org.hibernate.query.QueryParameter" );
	}

	@Override
	public void collectAllParameters(ParameterCollector collector) {
		if ( !hasNamedParameters() && !hasPositionalParameters() ) {
			return;
		}

		for ( QueryParameter queryParameter : namedQueryParameters.values() ) {
			collector.collect( queryParameter );
		}

		for ( QueryParameter queryParameter : positionalQueryParameters.values() ) {
			collector.collect( queryParameter );
		}
	}
}
