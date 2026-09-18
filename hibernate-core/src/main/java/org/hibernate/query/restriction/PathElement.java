/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * A non-root element of a {@link Path}.
 *
 * @author Gavin King
 */
record PathElement<X, U, V>(@Nonnull Path<? super X, U> parent, @Nonnull SingularAttribute<? super U, V> attribute)
		implements Path<X, V> {
	@Nonnull
	@Override
	public Class<V> getType() {
		return attribute.getJavaType();
	}

	@Nonnull
	@Override
	public jakarta.persistence.criteria.Path<V> path(@Nonnull Root<? extends X> root) {
		return parent.path( root ).get( attribute );
	}

	@Nonnull
	@Override
	public FetchParent<?, V> fetch(@Nonnull Root<? extends X> root) {
		return parent.fetch( root ).fetch( attribute, JoinType.LEFT );
	}
}
