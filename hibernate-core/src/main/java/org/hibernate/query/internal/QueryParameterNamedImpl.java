/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import org.hibernate.persister.common.spi.OrmTypeExporter;
import org.hibernate.sqm.query.Parameter;
import org.hibernate.type.spi.Type;

/**
 * QueryParameter impl for named-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class QueryParameterNamedImpl<T> extends AbstractQueryParameter<T> {
	/**
	 * Create a named parameter descriptor from the SQM parameter
	 *
	 * @param parameter The source parameter info
	 *
	 * @return The parameter descriptor
	 */
	public static <T> QueryParameterNamedImpl<T> fromSqm(Parameter parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;

		return new QueryParameterNamedImpl<T>(
				parameter.getName(),
				parameter.allowMultiValuedBinding(),
				true,
				( (OrmTypeExporter) parameter.getAnticipatedType() ).getOrmType()
		);
	}

	public static <T> QueryParameterNamedImpl<T> fromNativeQuery(String name) {
		return new QueryParameterNamedImpl<>(
				name,
				false,
				true,
				null
		);
	}

	private final String name;

	private QueryParameterNamedImpl(
			String name,
			boolean allowMultiValuedBinding,
			boolean isPassNullsEnabled,
			Type anticipatedType) {
		super( allowMultiValuedBinding, isPassNullsEnabled, anticipatedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
