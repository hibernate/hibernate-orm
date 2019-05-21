/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.produce.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmConsumeHelper {
	private SqmConsumeHelper() {
	}

	public static Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> generateJdbcParamsXref(
			DomainParameterXref domainParameterXref,
			JdbcParameterBySqmParameterAccess jdbcParameterBySqmParameterAccess) {
		if ( domainParameterXref == null || !domainParameterXref.hasParameters() ) {
			return Collections.emptyMap();
		}

		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> result = new IdentityHashMap<>();

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter>> entry :
				domainParameterXref.getSqmParamByQueryParam().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter> sqmParams = entry.getValue();

			final Map<SqmParameter, List<JdbcParameter>> sqmParamMap = result.computeIfAbsent(
					queryParam,
					qp -> new IdentityHashMap<>()
			);

			for ( SqmParameter sqmParam : sqmParams ) {
				sqmParamMap.put( sqmParam, jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam().get( sqmParam ) );
				result.put( queryParam, sqmParamMap );

				final List<SqmParameter> expansions = domainParameterXref.getExpansions( sqmParam );
				if ( ! expansions.isEmpty() ) {
					for ( SqmParameter expansion : expansions ) {
						sqmParamMap.put( expansion, jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam().get( expansion ) );
						result.put( queryParam, sqmParamMap );
					}
				}
			}
		}

		return result;
	}
}
