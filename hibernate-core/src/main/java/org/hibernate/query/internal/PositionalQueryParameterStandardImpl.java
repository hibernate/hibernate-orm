/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.sqm.domain.Type;

/**
 * QueryParameter impl for positional-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class PositionalQueryParameterStandardImpl<T> extends AbstractQueryParameter<T> {
	private final int position;

	public PositionalQueryParameterStandardImpl(Integer position, boolean allowMultiValuedBinding, Type anticipatedType) {
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
