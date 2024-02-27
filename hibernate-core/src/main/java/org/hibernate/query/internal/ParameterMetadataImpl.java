/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.LinkedIdentityHashMap;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.query.BindableType;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import jakarta.persistence.Parameter;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadataImplementor {
	/**
	 * Singleton access
	 */
	public static final ParameterMetadataImpl EMPTY = new ParameterMetadataImpl();

	private final Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> queryParameters;
	private final Map<String, QueryParameterImplementor<?>> queryParametersByName;
	private final Map<Integer, QueryParameterImplementor<?>> queryParametersByPosition;

	private ParameterMetadataImpl() {
		this.queryParameters = Collections.emptyMap();
		this.queryParametersByName = null;
		this.queryParametersByPosition = null;
	}

	public ParameterMetadataImpl(Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> queryParameters) {
		this.queryParameters = queryParameters;
		Map<String, QueryParameterImplementor<?>> tempQueryParametersByName = null;
		Map<Integer, QueryParameterImplementor<?>> tempQueryParametersByPosition = null;
		// if we have any ordinal parameters, make sure the numbers
		// start with 1 and are contiguous
		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
			if ( queryParameter.getPosition() != null ) {
				if ( tempQueryParametersByPosition == null ) {
					tempQueryParametersByPosition = new HashMap<>();
				}
				tempQueryParametersByPosition.put( queryParameter.getPosition(), queryParameter );
			}
			else if ( queryParameter.getName() != null ) {
				if ( tempQueryParametersByName == null ) {
					tempQueryParametersByName = new HashMap<>();
				}
				tempQueryParametersByName.put( queryParameter.getName(), queryParameter );
			}
		}

		if ( tempQueryParametersByPosition != null ) {
			verifyOrdinalParamLabels( tempQueryParametersByPosition.keySet() );
		}
		this.queryParametersByPosition = tempQueryParametersByPosition;
		this.queryParametersByName = tempQueryParametersByName;
	}

	public ParameterMetadataImpl(
			Map<Integer, QueryParameterImplementor<?>> positionalQueryParameters,
			Map<String, QueryParameterImplementor<?>> namedQueryParameters) {
		if ( CollectionHelper.isEmpty( positionalQueryParameters )
				&& CollectionHelper.isEmpty( namedQueryParameters ) ) {
			// no parameters
			this.queryParameters = Collections.emptyMap();
			this.queryParametersByName = null;
			this.queryParametersByPosition = null;
		}
		else {
			this.queryParameters = new LinkedIdentityHashMap<>();
			Map<String, QueryParameterImplementor<?>> tempQueryParametersByName = null;
			Map<Integer, QueryParameterImplementor<?>> tempQueryParametersByPosition = null;
			if ( positionalQueryParameters != null ) {
				for ( QueryParameterImplementor<?> value : positionalQueryParameters.values() ) {
					this.queryParameters.put( value, Collections.emptyList() );
					if ( tempQueryParametersByPosition == null ) {
						tempQueryParametersByPosition = new HashMap<>();
					}
					tempQueryParametersByPosition.put( value.getPosition(), value );
				}
				if ( tempQueryParametersByPosition != null ) {
					verifyOrdinalParamLabels( tempQueryParametersByPosition.keySet() );
				}
			}
			if ( namedQueryParameters != null ) {
				for ( QueryParameterImplementor<?> value : namedQueryParameters.values() ) {
					if ( tempQueryParametersByName == null ) {
						tempQueryParametersByName = new HashMap<>();
					}
					this.queryParameters.put( value, Collections.emptyList() );
					tempQueryParametersByName.put( value.getName(), value );
				}
			}
			this.queryParametersByPosition = tempQueryParametersByPosition;
			this.queryParametersByName = tempQueryParametersByName;
		}
	}

	private static void verifyOrdinalParamLabels(Set<Integer> labels) {
		if ( CollectionHelper.isEmpty( labels ) ) {
			return;
		}

		final List<Integer> sortedLabels = new ArrayList<>( labels );
		sortedLabels.sort( ComparableComparator.instance() );

		int lastPosition = -1;
		for ( Integer sortedPosition : sortedLabels ) {
			if ( lastPosition == -1 ) {
				if ( sortedPosition != 1 ) {
					throw new ParameterLabelException(
							String.format(
									Locale.ROOT,
									"Ordinal parameter labels start from '?%s' (ordinal parameters must be labelled from '?1')",
									sortedPosition
							)
					);
				}

				lastPosition = sortedPosition;
				continue;
			}

			if ( sortedPosition != lastPosition + 1 ) {
				throw new ParameterLabelException(
						String.format(
								Locale.ROOT,
								"Gap between '?%s' and '?%s' in ordinal parameter labels [%s] (ordinal parameters must be labelled sequentially)",
								lastPosition,
								sortedPosition,
								StringHelper.join( ",", sortedLabels.iterator() )
						)
				);
			}

			lastPosition = sortedPosition;
		}
	}

	@Override
	public int getParameterCount() {
		return queryParameters.size();
	}

	@Override
	public <T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter) {
		final List<SqmParameter<?>> sqmParameters = queryParameters.get( parameter );
		if ( sqmParameters == null || sqmParameters.isEmpty() ) {
			return null;
		}
		for ( SqmParameter<?> sqmParameter : sqmParameters ) {
			final BindableType<T> nodeType = (BindableType<T>) sqmParameter.getNodeType();
			if ( nodeType != null ) {
				return nodeType;
			}
		}
		return null;
	}

	@Override
	public boolean containsReference(QueryParameter<?> parameter) {
		//noinspection SuspiciousMethodCalls
		return queryParameters.containsKey( parameter );
	}

	@Override
	public void visitParameters(Consumer<QueryParameterImplementor<?>> consumer) {
		queryParameters.keySet().forEach( consumer );
	}

	@Override
	public Set<QueryParameterImplementor<?>> getRegistrations() {
		return Collections.unmodifiableSet( queryParameters.keySet() );
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
			if ( filter.test( queryParameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public <P> QueryParameterImplementor<P> resolve(Parameter<P> param) {
		if ( param instanceof QueryParameterImplementor ) {
			return (QueryParameterImplementor<P>) param;
		}

		final String errorMessage = "Could not resolve jakarta.persistence.Parameter '" + param + "' to org.hibernate.query.QueryParameter";
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named parameter handling

	@Override
	public boolean hasNamedParameters() {
		return queryParametersByName != null && !queryParametersByName.isEmpty();
	}

	@Override
	public Set<String> getNamedParameterNames() {
		if ( queryParametersByName == null ) {
			return Collections.EMPTY_SET;
		}
		return queryParametersByName.keySet();
	}

	@Override
	public QueryParameterImplementor<?> findQueryParameter(String name) {
		if ( queryParametersByName == null ) {
			return null;
		}
		return queryParametersByName.get( name );
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(String name) {
		final QueryParameterImplementor<?> parameter = findQueryParameter( name );

		if ( parameter != null ) {
			return parameter;
		}

		final String errorMessage = String.format(
				Locale.ROOT,
				"No parameter named ':%s' in query with named parameters [%s]",
				name,
				String.join( ", ", getNamedParameterNames() )
		);
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Positional parameter handling

	@Override
	public boolean hasPositionalParameters() {
		return queryParametersByPosition != null && !queryParametersByPosition.isEmpty();
	}

	public Set<Integer> getOrdinalParameterLabels() {
		if ( queryParametersByPosition == null ) {
			return Collections.EMPTY_SET;
		}
		return queryParametersByPosition.keySet();
	}

	@Override
	public QueryParameterImplementor<?> findQueryParameter(int positionLabel) {
		if(queryParametersByPosition == null){
			return null;
		}
		return queryParametersByPosition.get( positionLabel );
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(int positionLabel) {
		final QueryParameterImplementor<?> queryParameter = findQueryParameter( positionLabel );

		if ( queryParameter != null ) {
			return queryParameter;
		}

		final String errorMessage = String.format(
				Locale.ROOT,
				"No parameter labelled '?%s' in query with ordinal parameters [%s]",
				positionLabel,
				StringHelper.join( ", ", getOrdinalParameterLabels() )
		);
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
	}
}
