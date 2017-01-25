/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.sqm.query.SqmParameter;
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
	public static <T> QueryParameterNamedImpl<T> fromSqm(SqmParameter parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;

		return new QueryParameterNamedImpl<T>(
				parameter.getName(),
				parameter.allowMultiValuedBinding(),
				( (TypeExporter) parameter.getAnticipatedType() ).getOrmType()
		);
	}

	public static <T> QueryParameterNamedImpl<T> fromNativeQuery(String name) {
		return new QueryParameterNamedImpl<T>(
				name,
				false,
				null
		);
	}

	private final String name;

	private QueryParameterNamedImpl(
			String name,
			boolean allowMultiValuedBinding,
			Type anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
