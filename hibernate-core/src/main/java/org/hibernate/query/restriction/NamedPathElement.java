/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;

/**
 * A non-root element of a {@link Path}, using a stringly-typed
 * attribute reference.
 *
 * @author Gavin King
 */
record NamedPathElement<X, U, V>(Path<? super X, U> parent, String attributeName, Class<V> attributeType)
		implements Path<X, V> {
	@Override
	public Class<V> getType() {
		return attributeType;
	}

	@Override
	public jakarta.persistence.criteria.Path<V> path(Root<? extends X> root) {
		final jakarta.persistence.criteria.Path<V> path = parent.path( root ).get( attributeName );
		if ( !attributeType.isAssignableFrom( path.getJavaType() ) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName
												+ "' is not of type '" + attributeType.getName() + "'" );
		}
		return path;
	}

	@Override
	public FetchParent<?, V> fetch(Root<? extends X> root) {
		return parent.fetch( root ).fetch( attributeName, JoinType.LEFT );
	}
}
