/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * A non-root element of a {@link Path}.
 *
 * @author Gavin King
 */
record PathElement<X, U, V>(Path<? super X, U> parent, SingularAttribute<? super U, V> attribute)
		implements Path<X, V> {
	@Override
	public Class<V> getType() {
		return attribute.getJavaType();
	}

	@Override
	public jakarta.persistence.criteria.Path<V> path(Root<? extends X> root) {
		return parent.path( root ).get( attribute );
	}

	@Override
	public FetchParent<?, V> fetch(Root<? extends X> root) {
		return parent.fetch( root ).fetch( attribute, JoinType.LEFT );
	}
}
