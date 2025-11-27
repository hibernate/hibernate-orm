/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.query.internal.QueryParameterIdentifiedImpl;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.SqmTreeTransformationLogger;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.type.BasicCollectionType;

/**
 * Maintains a cross-reference between SqmParameter and QueryParameter references.
 *
 * @author Steve Ebersole
 */
public class DomainParameterXref {

	public static final DomainParameterXref EMPTY = new DomainParameterXref(
			new LinkedHashMap<>( 0 ),
			new IdentityHashMap<>( 0 ),
			SqmStatement.ParameterResolutions.empty()
	);

	/**
	 * Create a DomainParameterXref for the parameters defined in the SQM statement
	 */
	public static DomainParameterXref from(SqmStatement<?> sqmStatement) {
		final SqmStatement.ParameterResolutions parameterResolutions = sqmStatement.resolveParameters();
		if ( parameterResolutions.getSqmParameters().isEmpty() ) {
			return EMPTY;
		}

		final int sqmParamCount = parameterResolutions.getSqmParameters().size();
		final LinkedHashMap<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam = new LinkedHashMap<>( sqmParamCount );
		final IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam = new IdentityHashMap<>( sqmParamCount );

		for ( SqmParameter<?> sqmParameter : parameterResolutions.getSqmParameters() ) {
			if ( sqmParameter instanceof JpaCriteriaParameter ) {
				// see discussion on `SqmJpaCriteriaParameterWrapper#accept`
				throw new UnsupportedOperationException(
						"Unexpected JpaCriteriaParameter in SqmStatement#getSqmParameters.  Criteria parameters " +
								"should be represented as SqmJpaCriteriaParameterWrapper references in this collection"
				);
			}



			final QueryParameterImplementor<?> queryParameter;
			if ( sqmParameter.getName() != null ) {
				queryParameter = QueryParameterNamedImpl.fromSqm( sqmParameter );
			}
			else if ( sqmParameter.getPosition() != null ) {
				queryParameter = QueryParameterPositionalImpl.fromSqm( sqmParameter );
			}
			else if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				final SqmJpaCriteriaParameterWrapper<?> criteriaParameter = (SqmJpaCriteriaParameterWrapper<?>) sqmParameter;
				if ( sqmParameter.allowMultiValuedBinding()
						&& sqmParameter.getExpressible() != null
						&& sqmParameter.getExpressible().getSqmType() instanceof BasicCollectionType ) {
					// The wrapper parameter was inferred to be of a basic collection type,
					// so we disallow multivalued bindings, because binding a list of collections isn't useful
					criteriaParameter.getJpaCriteriaParameter().disallowMultiValuedBinding();
				}
				queryParameter = QueryParameterIdentifiedImpl.fromSqm( criteriaParameter );
			}
			else {
				throw new UnsupportedOperationException(
						"Unexpected SqmParameter type : " + sqmParameter );
			}

			sqmParamsByQueryParam.computeIfAbsent( queryParameter, impl -> new ArrayList<>() ).add( sqmParameter );
			queryParamBySqmParam.put( sqmParameter, queryParameter );
		}

		return new DomainParameterXref( sqmParamsByQueryParam, queryParamBySqmParam, parameterResolutions );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Instance state

	private final SqmStatement.ParameterResolutions parameterResolutions;

	private final LinkedHashMap<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam;
	private final IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam;

	private Map<SqmParameter<?>,List<SqmParameter<?>>> expansions;

	private DomainParameterXref(
			LinkedHashMap<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam,
			IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam,
			SqmStatement.ParameterResolutions parameterResolutions) {
		this.sqmParamsByQueryParam = sqmParamsByQueryParam;
		this.queryParamBySqmParam = queryParamBySqmParam;
		this.parameterResolutions = parameterResolutions;
	}

	public DomainParameterXref copy() {
		//noinspection unchecked
		return new DomainParameterXref(
				sqmParamsByQueryParam,
				(IdentityHashMap<SqmParameter<?>, QueryParameterImplementor<?>>) queryParamBySqmParam.clone(),
				parameterResolutions
		);
	}

	/**
	 * Does this xref contain any parameters?
	 */
	public boolean hasParameters() {
		return sqmParamsByQueryParam != null && ! sqmParamsByQueryParam.isEmpty();
	}

	/**
	 * Get all of the QueryParameters mapped by this xref
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

	public int getNumberOfSqmParameters(QueryParameterImplementor<?> queryParameter) {
		final List<SqmParameter<?>> sqmParameters = sqmParamsByQueryParam.get( queryParameter );
		if ( sqmParameters == null ) {
			// this should maybe be an exception instead
			return 0;
		}
		return sqmParameters.size();
	}

	public SqmStatement.ParameterResolutions getParameterResolutions() {
		return parameterResolutions;
	}

	public List<SqmParameter<?>> getSqmParameters(QueryParameterImplementor<?> queryParameter) {
		return sqmParamsByQueryParam.get( queryParameter );
	}

	public QueryParameterImplementor<?> getQueryParameter(SqmParameter<?> sqmParameter) {
		if ( sqmParameter instanceof QueryParameterImplementor<?> ) {
			return (QueryParameterImplementor<?>) sqmParameter;
		}
		return queryParamBySqmParam.get( sqmParameter );
	}

	public void addExpansion(
			QueryParameterImplementor<?> domainParam,
			SqmParameter originalSqmParameter,
			SqmParameter expansion) {
		assert !queryParamBySqmParam.isEmpty();
		queryParamBySqmParam.put( expansion, domainParam );

		if ( expansions == null ) {
			expansions = new IdentityHashMap<>();
		}

		expansions.computeIfAbsent( originalSqmParameter, p -> new ArrayList<>() ).add( expansion );
	}

	public List<SqmParameter<?>> getExpansions(SqmParameter<?> sqmParameter) {
		if ( expansions == null ) {
			return Collections.emptyList();
		}

		final List<SqmParameter<?>> sqmParameters = expansions.get( sqmParameter );
		return sqmParameters == null ? Collections.emptyList() : sqmParameters;
	}

	public void clearExpansions() {
		if ( expansions == null ) {
			return;
		}

		for ( List<SqmParameter<?>> expansionList : expansions.values() ) {
			for ( SqmParameter<?> expansion : expansionList ) {
				queryParamBySqmParam.remove( expansion );
			}
		}

		expansions.clear();
	}
}
