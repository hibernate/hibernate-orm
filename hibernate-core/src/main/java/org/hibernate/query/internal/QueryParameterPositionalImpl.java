/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.query.QueryParameter;
import org.hibernate.sqm.domain.Type;
import org.hibernate.sqm.query.Parameter;

/**
 * QueryParameter impl for positional-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class QueryParameterPositionalImpl<T> extends AbstractQueryParameter<T> {
	/**
	 * Create a positional parameter descriptor from the SQM parameter
	 *
	 * @param parameter The source parameter info
	 *
	 * @return The parameter descriptor
	 */
	public static <T> QueryParameter<T> fromSqm(Parameter parameter) {
		assert parameter.getPosition() != null;
		assert parameter.getName() == null;

		return new QueryParameterPositionalImpl<>(
				parameter.getPosition(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType()
		);
	}

	public static <T> QueryParameterPositionalImpl<T> fromNativeQuery(int position) {
		return new QueryParameterPositionalImpl<>(
				position,
				false,
				null
		);
	}

	private final int position;

	private QueryParameterPositionalImpl(Integer position, boolean allowMultiValuedBinding, Type anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.position = position;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public boolean isJpaPositionalParameter() {
		return true;
	}
}
