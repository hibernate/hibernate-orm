/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.BindableType;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import jakarta.persistence.Parameter;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

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
	private final @Nullable QueryParameterBindingsImpl queryParameterBindingsTemplate;

	private ParameterMetadataImpl() {
		queryParameters = emptyMap();
		queryParametersByName = null;
		queryParametersByPosition = null;
		queryParameterBindingsTemplate = null;
	}

	public ParameterMetadataImpl(Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> queryParameters) {
		assert !queryParameters.isEmpty();
		this.queryParameters = queryParameters;
		Map<String, QueryParameterImplementor<?>> tempQueryParametersByName = null;
		Map<Integer, QueryParameterImplementor<?>> tempQueryParametersByPosition = null;
		// if we have any ordinal parameters, make sure the numbers
		// start with 1 and are contiguous
		for ( var queryParameter : queryParameters.keySet() ) {
			if ( queryParameter.isOrdinal() ) {
				if ( tempQueryParametersByPosition == null ) {
					tempQueryParametersByPosition = new HashMap<>();
				}
				tempQueryParametersByPosition.put( queryParameter.getPosition(), queryParameter );
			}
			else if ( queryParameter.isNamed() ) {
				if ( tempQueryParametersByName == null ) {
					tempQueryParametersByName = new HashMap<>();
				}
				tempQueryParametersByName.put( queryParameter.getName(), queryParameter );
			}
		}

		if ( tempQueryParametersByPosition != null ) {
			verifyOrdinalParamLabels( tempQueryParametersByPosition.keySet() );
		}
		queryParametersByPosition = tempQueryParametersByPosition;
		queryParametersByName = tempQueryParametersByName;
		queryParameterBindingsTemplate = QueryParameterBindingsImpl.from( this, null );
	}

	public ParameterMetadataImpl(
			Map<Integer, QueryParameterImplementor<?>> positionalQueryParameters,
			Map<String, QueryParameterImplementor<?>> namedQueryParameters) {
		assert !isEmpty( positionalQueryParameters ) || !isEmpty( namedQueryParameters );
		queryParameters = new LinkedHashMap<>();
		Map<String, QueryParameterImplementor<?>> tempQueryParametersByName = null;
		Map<Integer, QueryParameterImplementor<?>> tempQueryParametersByPosition = null;
		if ( positionalQueryParameters != null ) {
			for ( var queryParameter : positionalQueryParameters.values() ) {
				queryParameters.put( queryParameter, emptyList() );
				if ( tempQueryParametersByPosition == null ) {
					tempQueryParametersByPosition = new HashMap<>();
				}
				tempQueryParametersByPosition.put( queryParameter.getPosition(), queryParameter );
			}
			if ( tempQueryParametersByPosition != null ) {
				verifyOrdinalParamLabels( tempQueryParametersByPosition.keySet() );
			}
		}
		if ( namedQueryParameters != null ) {
			for ( var queryParameter : namedQueryParameters.values() ) {
				if ( tempQueryParametersByName == null ) {
					tempQueryParametersByName = new HashMap<>();
				}
				queryParameters.put( queryParameter, emptyList() );
				tempQueryParametersByName.put( queryParameter.getName(), queryParameter );
			}
		}
		queryParametersByPosition = tempQueryParametersByPosition;
		queryParametersByName = tempQueryParametersByName;
		queryParameterBindingsTemplate = QueryParameterBindingsImpl.from( this, null );
	}

	private static void verifyOrdinalParamLabels(Set<Integer> labels) {
		if ( !isEmpty( labels ) ) {
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
				}
				else {
					if ( sortedPosition != lastPosition + 1 ) {
						throw new ParameterLabelException(
								String.format(
										Locale.ROOT,
										"Gap between '?%s' and '?%s' in ordinal parameter labels [%s] (ordinal parameters must be labelled sequentially)",
										lastPosition,
										sortedPosition,
										join( ",", sortedLabels.iterator() )
								)
						);
					}
				}
				lastPosition = sortedPosition;
			}
		}
	}

	@Override
	public Collection<QueryParameter<?>> getParameters() {
		return unmodifiableSet( queryParameters.keySet() );
	}

	@Override
	public QueryParameterBindings createBindings(SessionFactoryImplementor sessionFactory) {
		return queryParameterBindingsTemplate == null
				? QueryParameterBindingsImpl.EMPTY
				: queryParameterBindingsTemplate.copyWithoutValues( sessionFactory );
	}

	@Override
	public int getParameterCount() {
		return queryParameters.size();
	}

	@Override
	public <T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter) {
		final var sqmParameters = queryParameters.get( (QueryParameterImplementor<T>) parameter );
		if ( sqmParameters == null || sqmParameters.isEmpty() ) {
			return null;
		}
		for ( var sqmParameter : sqmParameters ) {
			final var nodeType = sqmParameter.getNodeType();
			if ( nodeType != null ) {
				//noinspection unchecked
				return (BindableType<T>) nodeType;
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
	public void visitParameters(Consumer<QueryParameter<?>> consumer) {
		queryParameters.keySet().forEach( consumer );
	}

	@Override
	public Set<QueryParameterImplementor<?>> getRegistrations() {
		return unmodifiableSet( queryParameters.keySet() );
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		for ( var queryParameter : queryParameters.keySet() ) {
			if ( filter.test( queryParameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public <P> QueryParameterImplementor<P> resolve(Parameter<P> param) {
		if ( param instanceof QueryParameterImplementor<P> parameterImplementor ) {
			return parameterImplementor;
		}

		final String errorMessage =
				"Could not resolve jakarta.persistence.Parameter '" + param + "' to org.hibernate.query.QueryParameter";
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
		return queryParametersByName == null ? emptySet() : queryParametersByName.keySet();
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
		final var parameter = findQueryParameter( name );
		if ( parameter != null ) {
			return parameter;
		}
		else {
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
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Positional parameter handling

	@Override
	public boolean hasPositionalParameters() {
		return queryParametersByPosition != null && !queryParametersByPosition.isEmpty();
	}

	public Set<Integer> getOrdinalParameterLabels() {
		return queryParametersByPosition == null ? emptySet() : queryParametersByPosition.keySet();
	}

	@Override
	public QueryParameterImplementor<?> findQueryParameter(int positionLabel) {
		return queryParametersByPosition == null ? null : queryParametersByPosition.get( positionLabel );
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(int positionLabel) {
		final var queryParameter = findQueryParameter( positionLabel );
		if ( queryParameter != null ) {
			return queryParameter;
		}
		else {
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
}
