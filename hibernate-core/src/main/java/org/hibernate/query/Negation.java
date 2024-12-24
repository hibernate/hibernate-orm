/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Negates a restriction; a logical NOT.
 *
 * @param restriction The restriction to be negated
 * @param <X> The entity type
 */
record Negation<X>(Restriction<X> restriction) implements Restriction<X> {
	@Override
	public Restriction<X> negated() {
		return restriction;
	}

	@Override
	public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
		return builder.not( restriction.toPredicate( root, builder ) );
	}
}
