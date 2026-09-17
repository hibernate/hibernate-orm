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
 * A compound restriction constructed using logical OR.
 *
 * @param restrictions The restrictions to be OR-ed
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record Disjunction<X>(@Nonnull java.util.List<? extends Restriction<? super X>> restrictions)
		implements Restriction<X> {
	@Nonnull
	@Override
	public Restriction<X> negated() {
		return new Conjunction<>( restrictions.stream().map( Restriction::negated ).toList() );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Root<? extends X> root, @Nonnull CriteriaBuilder builder) {
		return builder.or( restrictions.stream()
				.map( restriction -> restriction.toPredicate( root, builder ) )
				.toList() );
	}
}
