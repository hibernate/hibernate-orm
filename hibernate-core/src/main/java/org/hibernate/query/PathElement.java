/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * A non-root element of a {@link Path}.
 *
 * @author Gavin King
 */
record PathElement<X, U, V>(Path<? super X, U> parent, SingularAttribute<? super U, V> attribute) implements Path<X, V> {
	@Override
	public jakarta.persistence.criteria.Path<V> path(Root<? extends X> root) {
		return parent.path( root ).get( attribute );
	}
}
