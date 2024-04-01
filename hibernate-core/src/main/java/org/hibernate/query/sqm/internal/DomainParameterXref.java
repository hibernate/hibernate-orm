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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.internal.util.collections.LinkedIdentityHashMap;
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
	/**
	 * Create a DomainParameterXref for the parameters defined in the SQM statement
	 */
	public static DomainParameterXref from(SqmStatement<?> sqmStatement) {
		// `xrefMap` is used to help maintain the proper cardinality between an
		// SqmParameter and a QueryParameter.  Multiple SqmParameter references
		// can map to the same QueryParameter.  Consider, e.g.,
		// `.. where a.b = :param or a.c = :param`.  Here we have 2 SqmParameter
		// references (one for each occurrence of `:param`) both of which map to
		// the same QueryParameter.
		final Map<SqmParameter<?>, QueryParameterImplementor<?>> xrefMap = new TreeMap<>();

		final SqmStatement.ParameterResolutions parameterResolutions = sqmStatement.resolveParameters();
		if ( parameterResolutions.getSqmParameters().isEmpty() ) {
			return empty();
		}

		final Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam = new LinkedIdentityHashMap<>();

		final int sqmParamCount = parameterResolutions.getSqmParameters().size();
		final Map<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam = new IdentityHashMap<>( sqmParamCount );

		for ( SqmParameter<?> sqmParameter : parameterResolutions.getSqmParameters() ) {
			if ( sqmParameter instanceof JpaCriteriaParameter ) {
				// see discussion on `SqmJpaCriteriaParameterWrapper#accept`
				throw new UnsupportedOperationException(
						"Unexpected JpaCriteriaParameter in SqmStatement#getSqmParameters.  Criteria parameters " +
								"should be represented as SqmJpaCriteriaParameterWrapper references in this collection"
				);
			}

			final QueryParameterImplementor<?> queryParameter = xrefMap.computeIfAbsent(
					sqmParameter,
					p -> {
						if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper ) {
							return ( (SqmJpaCriteriaParameterWrapper<?>) sqmParameter ).getJpaCriteriaParameter();
						}
						else if ( sqmParameter.getName() != null ) {
							return QueryParameterNamedImpl.fromSqm( sqmParameter );
						}
						else if ( sqmParameter.getPosition() != null ) {
							return QueryParameterPositionalImpl.fromSqm( sqmParameter );
						}
						else {
							throw new UnsupportedOperationException( "Unexpected SqmParameter type : " + sqmParameter );
						}
					}
			);

			if ( ! sqmParameter.allowMultiValuedBinding() ) {
				if ( queryParameter.allowsMultiValuedBinding() ) {
					SqmTreeTransformationLogger.LOGGER.debugf(
							"SqmParameter [%s] does not allow multi-valued binding, " +
									"but mapped to existing QueryParameter [%s] that does - " +
									"disallowing multi-valued binding" ,
							sqmParameter,
							queryParameter
					);
					queryParameter.disallowMultiValuedBinding();
				}
			}
			else if ( sqmParameter.getExpressible() != null && sqmParameter.getExpressible().getSqmType() instanceof BasicCollectionType ) {
				queryParameter.disallowMultiValuedBinding();
			}

			sqmParamsByQueryParam.computeIfAbsent( queryParameter, qp -> new ArrayList<>() ).add( sqmParameter );
			queryParamBySqmParam.put( sqmParameter, queryParameter );
		}

		return new DomainParameterXref( sqmParamsByQueryParam, queryParamBySqmParam, parameterResolutions );
	}

	/**
	 * Creates an "empty" (no param) xref
	 */
	public static DomainParameterXref empty() {
		return new DomainParameterXref( Collections.emptyMap(), Collections.emptyMap(), SqmStatement.ParameterResolutions.empty() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Instance state

	private final SqmStatement.ParameterResolutions parameterResolutions;

	private final Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam;
	private final Map<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam;

	private Map<SqmParameter<?>,List<SqmParameter<?>>> expansions;

	/**
	 * @implSpec Constructor is defined as public for
	 */
	public DomainParameterXref(
			Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> sqmParamsByQueryParam,
			Map<SqmParameter<?>, QueryParameterImplementor<?>> queryParamBySqmParam,
			SqmStatement.ParameterResolutions parameterResolutions) {
		this.sqmParamsByQueryParam = sqmParamsByQueryParam;
		this.queryParamBySqmParam = queryParamBySqmParam;
		this.parameterResolutions = parameterResolutions;
	}

	public DomainParameterXref copy() {
		return new DomainParameterXref(
				sqmParamsByQueryParam,
				new IdentityHashMap<>( queryParamBySqmParam ),
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
		if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper ) {
			return ( (SqmJpaCriteriaParameterWrapper<?>) sqmParameter ).getJpaCriteriaParameter();
		}
		else if ( sqmParameter instanceof QueryParameterImplementor<?> ) {
			return (QueryParameterImplementor<?>) sqmParameter;
		}
		return queryParamBySqmParam.get( sqmParameter );
	}

	public void addExpansion(
			QueryParameterImplementor<?> domainParam,
			SqmParameter originalSqmParameter,
			SqmParameter expansion) {
		SqmTreeTransformationLogger.LOGGER.debugf( "Adding domain-param xref expansion : %s", originalSqmParameter );
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
