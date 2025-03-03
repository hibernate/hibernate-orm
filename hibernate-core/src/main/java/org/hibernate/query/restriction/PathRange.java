/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.range.Range;

/**
 * Restricts a path from an entity to a given {@link Range}.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 *
 * @author Gavin King
 */
record PathRange<X, U>(Path<X, U> path, Range<? super U> range) implements Restriction<X> {
	@Override
	public Restriction<X> negated() {
		return new Negation<>( this );
	}

	@Override
	public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
		return range.toPredicate( path.path( root ), builder );
	}
}
