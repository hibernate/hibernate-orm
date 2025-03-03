/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.range.Range;

/**
 * Restricts an attribute of an entity to a given {@link Range}.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 *
 * @author Gavin King
 */
record AttributeRange<X, U>(SingularAttribute<X, U> attribute, Range<U> range) implements Restriction<X> {
	@Override
	public Restriction<X> negated() {
		return new Negation<>( this );
	}

	@Override
	public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
		return range.toPredicate( root.get( attribute ), builder );
	}
}
