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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.criteria.sqm.JpaParameterSqmWrapper;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;

/**
 * Maintains a cross-reference between SqmParameter and QueryParameter references.
 *
 * @apiNote The difference between {@link #addCriteriaAdjustment} and {@link #addExpansion}
 * is the durability of given parameter.  A Criteria-adjustment lives beyond
 * {@link #clearExpansions()} while an expansion does not.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DomainParameterXref {
	/**
	 * Create a DomainParameterXref for the parameters defined in the passed
	 * SQM statement
	 */
	public static DomainParameterXref from(SqmStatement sqmStatement) {
		final Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam = new IdentityHashMap<>();
		final Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam = new IdentityHashMap<>();

		// `xrefMap` is used to help maintain the proper cardinality between an
		// SqmParameter and a QueryParameter.  Multiple SqmParameter references
		// can map to the same QueryParameter.  Consider, e.g.,
		// `.. where a.b = :param or a.c = :param`.  Here we have 2 SqmParameter
		// references (one for each occurrence of `:param`) both of which map to
		// the same QueryParameter.
		final Map<SqmParameter,QueryParameterImplementor<?>> xrefMap = new TreeMap<>(
				(o1, o2) -> {
					if ( o1 instanceof SqmNamedParameter ) {
						final SqmNamedParameter one = (SqmNamedParameter) o1;
						final SqmNamedParameter another = (SqmNamedParameter) o2;

						return one.getName().compareTo( another.getName() );
					}
					else if ( o1 instanceof SqmPositionalParameter ) {
						final SqmPositionalParameter one = (SqmPositionalParameter) o1;
						final SqmPositionalParameter another = (SqmPositionalParameter) o2;

						return one.getPosition().compareTo( another.getPosition() );
					}
					else if ( o1 instanceof JpaParameterSqmWrapper ) {
						if ( o1.getName() != null ) {
							return o1.getName().compareTo( o2.getName() );
						}
						else if ( o1.getPosition() != null ) {
							return o1.getPosition().compareTo( o2.getPosition() );
						}
						else {
							return Integer.compare( o1.hashCode(), o2.hashCode() );
						}
					}

					throw new HibernateException( "Unexpected SqmParameter type for comparison : " + o1 + " & " + o2 );
				}
		);

		for ( SqmParameter sqmParameter : sqmStatement.getSqmParameters() ) {
			final QueryParameterImplementor<?> queryParameter = xrefMap.computeIfAbsent(
					sqmParameter,
					p -> {
						if ( sqmParameter instanceof JpaParameterSqmWrapper ) {
							return ( (JpaParameterSqmWrapper) sqmParameter ).getJpaParameterExpression();
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
					QueryLogger.QUERY_LOGGER.debugf(
							"SqmParameter [%s] does not allow multi-valued binding, " +
									"but mapped to existing QueryParameter [%s] that does - " +
									"disallowing multi-valued binding" ,
							sqmParameter,
							queryParameter
					);
					queryParameter.disallowMultiValuedBinding();
				}
			}

			sqmParamsByQueryParam.computeIfAbsent( queryParameter, qp -> new ArrayList<>() ).add( sqmParameter );
			queryParamBySqmParam.put( sqmParameter, queryParameter );
		}

		return new DomainParameterXref( sqmParamsByQueryParam, queryParamBySqmParam );
	}

	/**
	 * Creates an "empty" (no param) xref
	 */
	public static DomainParameterXref empty() {
		return new DomainParameterXref( Collections.emptyMap(), Collections.emptyMap() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Instance state

	private final Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam;
	private final Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam;

	private Map<SqmParameter,List<SqmParameter>> expansions;

	/**
	 * @implSpec Constructor is defined as public for
	 */
	public DomainParameterXref(
			Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam,
			Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam) {
		this.sqmParamsByQueryParam = sqmParamsByQueryParam;
		this.queryParamBySqmParam = queryParamBySqmParam;
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
	public Set<QueryParameterImplementor<?>> getQueryParameters() {
		return sqmParamsByQueryParam.keySet();
	}

	/**
	 * Get the mapping of all QueryParameters to the List of its corresponding
	 * SqmParameters
	 */
	public Map<QueryParameterImplementor<?>, List<SqmParameter>> getSqmParamByQueryParam() {
		return sqmParamsByQueryParam;
	}

	public List<SqmParameter> getSqmParameters(QueryParameterImplementor<?> queryParameter) {
		return sqmParamsByQueryParam.get( queryParameter );
	}

	public QueryParameterImplementor<?> getQueryParameter(SqmParameter sqmParameter) {
		return queryParamBySqmParam.get( sqmParameter );
	}

	public void addCriteriaAdjustment(
			QueryParameterImplementor<?> domainParam,
			JpaParameterSqmWrapper originalSqmParameter,
			SqmParameter adjustment) {
		QueryLogger.QUERY_LOGGER.debugf( "Adding JPA-param xref adjustment : %s", originalSqmParameter );
		sqmParamsByQueryParam.get( domainParam ).add( adjustment );
		queryParamBySqmParam.put( adjustment, domainParam );
	}

	public void addExpansion(
			QueryParameterImplementor<?> domainParam,
			SqmParameter originalSqmParameter,
			SqmParameter expansion) {
		QueryLogger.QUERY_LOGGER.debugf( "Adding domain-param xref expansion : %s", originalSqmParameter );
		queryParamBySqmParam.put( expansion, domainParam );

		if ( expansions == null ) {
			expansions = new IdentityHashMap<>();
		}

		expansions.computeIfAbsent( originalSqmParameter, p -> new ArrayList<>() ).add( expansion );
	}

	public List<SqmParameter> getExpansions(SqmParameter sqmParameter) {
		if ( expansions == null ) {
			return Collections.emptyList();
		}

		final List<SqmParameter> sqmParameters = expansions.get( sqmParameter );
		return sqmParameters == null ? Collections.emptyList() : sqmParameters;
	}

	public void clearExpansions() {
		if ( expansions == null ) {
			return;
		}

		for ( List<SqmParameter> expansionList : expansions.values() ) {
			for ( SqmParameter expansion : expansionList ) {
				queryParamBySqmParam.remove( expansion );
			}
		}

		expansions.clear();
	}
}
