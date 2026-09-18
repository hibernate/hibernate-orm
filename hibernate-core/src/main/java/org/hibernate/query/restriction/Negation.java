/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Negates a restriction; a logical NOT.
 *
 * @param restriction The restriction to be negated
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record Negation<X>(@Nonnull Restriction<X> restriction) implements Restriction<X> {
	@Nonnull
	@Override
	public Restriction<X> negated() {
		return restriction;
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Root<? extends X> root, @Nonnull CriteriaBuilder builder) {
		return builder.not( restriction.toPredicate( root, builder ) );
	}
}
