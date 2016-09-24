/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.old;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Parameter;

import org.hibernate.QueryParameterException;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class OldParameterMetadataImpl implements ParameterMetadata {
	private final Map<String,QueryParameter> namedQueryParameters;
	private final Map<Integer,QueryParameter> positionalQueryParameters;

	/**
	 * Instantiates a ParameterMetadata container.
	 */
	public OldParameterMetadataImpl(
			Map<String, QueryParameter> namedQueryParameters,
			Map<Integer, QueryParameter> positionalQueryParameters) {
		validatePositionalParameters( positionalQueryParameters );

		this.namedQueryParameters = namedQueryParameters != null ? namedQueryParameters : Collections.emptyMap();
		this.positionalQueryParameters = positionalQueryParameters != null ? positionalQueryParameters : Collections.emptyMap();
	}

	private static void validatePositionalParameters(Map<Integer, QueryParameter> positionalQueryParameters) {
		if ( positionalQueryParameters == null ) {
			return;
		}

		// validate the positions.  JPA says that these should start with 1 and
		// increment contiguously (no gaps)
		int[] positionsArray = positionalQueryParameters.keySet().stream().mapToInt( Integer::intValue ).toArray();
		Arrays.sort( positionsArray );

		int previous = 0;
		for ( Integer position : positionsArray ) {
			if ( position != previous + 1 ) {
				if ( previous == 0 ) {
					throw new QueryParameterException( "Positional parameters did not start with 1 : " + position );
				}
				else {
					throw new QueryParameterException( "Gap in positional parameter positions; skipped " + (previous+1) );
				}
			}
			previous = position;
		}
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
	@SuppressWarnings("unchecked")
	public Set<QueryParameter<?>> collectAllParameters() {
		if ( !hasNamedParameters() && !hasPositionalParameters() ) {
			return Collections.emptySet();
		}

		final HashSet allParameters = new HashSet();
		allParameters.addAll( namedQueryParameters.values() );
		allParameters.addAll( positionalQueryParameters.values() );
		return allParameters;
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return  namedQueryParameters.keySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> collectAllParametersJpa() {
		if ( !hasNamedParameters() && !hasPositionalParameters() ) {
			return Collections.emptySet();
		}

		final HashSet allParameters = new HashSet();
		allParameters.addAll( namedQueryParameters.values() );
		allParameters.addAll( positionalQueryParameters.values() );
		return allParameters;
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
}
