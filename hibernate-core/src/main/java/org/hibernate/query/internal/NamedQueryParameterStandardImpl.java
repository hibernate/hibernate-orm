/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.sqm.domain.Type;

/**
 * QueryParameter impl for named-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class NamedQueryParameterStandardImpl<T> extends AbstractQueryParameter<T> {
	private final String name;

	public NamedQueryParameterStandardImpl(String name, boolean allowMultiValuedBinding, Type anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
