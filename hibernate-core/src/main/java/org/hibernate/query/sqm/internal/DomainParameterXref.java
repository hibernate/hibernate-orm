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
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.query.criteria.sqm.JpaParameterSqmWrapper;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DomainParameterXref {
	/**
	 * Create a DomainParameterXref for the parameters defined in the passed
	 * SQM statement
	 */
	public static DomainParameterXref from(SqmStatement sqmStatement) {
		// `xrefMap` is used to help maintain the proper cardinality between an
		// SqmParameter and a QueryParameter.  Multiple SqmParameter references
		// can map to the same QueryParameter.  Consider, e.g.,
		// `.. where a.b = :param or a.c = :param`.  Here we have 2 SqmParameter
		// references (one for each occurrence of `:p`) that both map to the same
		// QueryParameter.
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
						else {
							return o1.getPosition().compareTo( o2.getPosition() );
						}
					}

					throw new HibernateException( "Unexpected SqmParameter type for comparison : " + o1 + " & " + o2 );
				}
		);

		final Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam = new IdentityHashMap<>();
		final Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam = new IdentityHashMap<>();

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

			sqmParamsByQueryParam.computeIfAbsent( queryParameter, qp -> new ArrayList<>() ).add( sqmParameter );
			queryParamBySqmParam.put( sqmParameter, queryParameter );
		}

		return new DomainParameterXref( sqmParamsByQueryParam, queryParamBySqmParam );
	}

	public static DomainParameterXref empty() {
		return new DomainParameterXref( Collections.emptyMap(), Collections.emptyMap() );
	}

	private final Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam;
	private final Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam;

	public DomainParameterXref(
			Map<QueryParameterImplementor<?>, List<SqmParameter>> sqmParamsByQueryParam,
			Map<SqmParameter, QueryParameterImplementor<?>> queryParamBySqmParam) {
		this.sqmParamsByQueryParam = sqmParamsByQueryParam;
		this.queryParamBySqmParam = queryParamBySqmParam;
	}

	public boolean hasParameters() {
		return sqmParamsByQueryParam != null && ! sqmParamsByQueryParam.isEmpty();
	}

	public Set<QueryParameterImplementor<?>> getQueryParameters() {
		return sqmParamsByQueryParam.keySet();
	}

	public Map<QueryParameterImplementor<?>, List<SqmParameter>> getSqmParamByQueryParam() {
		return sqmParamsByQueryParam;
	}

	public Map<SqmParameter, QueryParameterImplementor<?>> getQueryParamBySqmParam() {
		return queryParamBySqmParam;
	}
}
