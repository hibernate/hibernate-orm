/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.query.internal.QueryParameterIdentifiedImpl;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.type.BasicCollectionType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * Maintains a cross-reference between SqmParameter and QueryParameter references.
 *
 * @author Steve Ebersole
 */
public class DomainParameterXref {

	public static final DomainParameterXref EMPTY = new DomainParameterXref();

	/**
	 * Create a DomainParameterXref for the parameters defined in the SQM statement
	 */
	public static DomainParameterXref from(SqmStatement<?> sqmStatement) {
		final var parameterResolutions = sqmStatement.resolveParameters();
		final var parameters = parameterResolutions.getSqmParameters();
		return parameters.isEmpty()
				? EMPTY
				: new DomainParameterXref( parameterResolutions, parameters );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Instance state

	private final SqmStatement.ParameterResolutions parameterResolutions;

	private final LinkedHashMap<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam;
	private final IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam;

	private Map<SqmParameter<?>,List<SqmParameter<?>>> expansions;

	private DomainParameterXref() {
		sqmParamsByQueryParam = new LinkedHashMap<>( 0 );
		queryParamBySqmParam = new IdentityHashMap<>( 0 );
		parameterResolutions = SqmStatement.ParameterResolutions.empty();
	}

	private DomainParameterXref(DomainParameterXref that) {
		sqmParamsByQueryParam = that.sqmParamsByQueryParam;
		//noinspection unchecked
		queryParamBySqmParam =
				(IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>>)
						that.queryParamBySqmParam.clone();
		parameterResolutions = that.parameterResolutions;
	}

	private DomainParameterXref(
			SqmStatement.ParameterResolutions resolutions,
			Set<SqmParameter<?>> parameters) {
		parameterResolutions = resolutions;
		final int sqmParamCount = parameters.size();
		sqmParamsByQueryParam = new LinkedHashMap<>( sqmParamCount );
		queryParamBySqmParam = new IdentityHashMap<>( sqmParamCount );

		for ( var parameter : parameters ) {
			if ( parameter instanceof JpaCriteriaParameter ) {
				// see discussion on `SqmJpaCriteriaParameterWrapper#accept`
				throw new UnsupportedOperationException(
						"Unexpected JpaCriteriaParameter (criteria parameters should be represented as SqmJpaCriteriaParameterWrapper references in this collection)"
				);
			}

			final var queryParameter = fromSqm( parameter );
			sqmParamsByQueryParam.computeIfAbsent( queryParameter, impl -> new ArrayList<>() )
					.add( parameter );
			queryParamBySqmParam.put( parameter, queryParameter );
		}
	}

	private static @NonNull QueryParameterImplementor<?> fromSqm(SqmParameter<?> sqmParameter) {
		if ( sqmParameter.getName() != null ) {
			return QueryParameterNamedImpl.fromSqm( sqmParameter );
		}
		else if ( sqmParameter.getPosition() != null ) {
			return QueryParameterPositionalImpl.fromSqm( sqmParameter );
		}
		else if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> criteriaParameter ) {
			if ( sqmParameter.allowMultiValuedBinding() ) {
				final var expressible = sqmParameter.getExpressible();
				if ( expressible != null && expressible.getSqmType() instanceof BasicCollectionType ) {
					// The wrapper parameter was inferred to be of a basic
					// collection type, so we disallow multivalued bindings,
					// because binding a list of collections isn't useful
					criteriaParameter.getJpaCriteriaParameter().disallowMultiValuedBinding();
				}
			}
			return QueryParameterIdentifiedImpl.fromSqm( criteriaParameter );
		}
		else {
			throw new UnsupportedOperationException( "Unexpected SqmParameter type: " + sqmParameter );
		}
	}

	public DomainParameterXref copy() {
		return new DomainParameterXref( this );
	}

	/**
	 * Does this xref contain any parameters?
	 */
	public boolean hasParameters() {
		return sqmParamsByQueryParam != null
			&& ! sqmParamsByQueryParam.isEmpty();
	}

	/**
	 * Get all the QueryParameters mapped by this xref.
	 * Note that order of parameters is important - parameters are
	 * included in cache keys for query results caching.
	 */
	public Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> getQueryParameters() {
		return sqmParamsByQueryParam;
	}

	public int getQueryParameterCount() {
		return sqmParamsByQueryParam.size();
	}

	public int getSqmParameterCount() {
		return queryParamBySqmParam.size();
	}

	public SqmStatement.ParameterResolutions getParameterResolutions() {
		return parameterResolutions;
	}

	public List<SqmParameter<?>> getSqmParameters(QueryParameterImplementor<?> queryParameter) {
		return sqmParamsByQueryParam.get( queryParameter );
	}

	public QueryParameterImplementor<?> getQueryParameter(SqmParameter<?> sqmParameter) {
		return sqmParameter instanceof QueryParameterImplementor<?> parameterImplementor
				? parameterImplementor
				: queryParamBySqmParam.get( sqmParameter );
	}

	public void addExpansion(
			QueryParameterImplementor<?> domainParam,
			SqmParameter<?> originalSqmParameter,
			SqmParameter<?> expansion) {
		assert !queryParamBySqmParam.isEmpty();
		queryParamBySqmParam.put( expansion, domainParam );
		if ( expansions == null ) {
			expansions = new IdentityHashMap<>();
		}
		expansions.computeIfAbsent( originalSqmParameter, p -> new ArrayList<>() ).add( expansion );
	}

	public List<SqmParameter<?>> getExpansions(SqmParameter<?> sqmParameter) {
		if ( expansions == null ) {
			return emptyList();
		}
		else {
			final var sqmParameters = expansions.get( sqmParameter );
			return sqmParameters == null ? emptyList() : sqmParameters;
		}
	}

	public void clearExpansions() {
		if ( expansions != null ) {
			for ( var expansionList : expansions.values() ) {
				for ( var expansion : expansionList ) {
					queryParamBySqmParam.remove( expansion );
				}
			}
			expansions.clear();
		}
	}
}
