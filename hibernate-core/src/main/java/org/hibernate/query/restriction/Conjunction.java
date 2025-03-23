/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A compound restriction constructed using logical AND.
 *
 * @param restrictions The restrictions to be AND-ed
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record Conjunction<X>(java.util.List<? extends Restriction<? super X>> restrictions)
		implements Restriction<X> {
	@Override
	public Restriction<X> negated() {
		return new Disjunction<>( restrictions.stream().map( Restriction::negated ).toList() );
	}

	@Override
	public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
		return builder.and( restrictions.stream()
				.map( restriction -> restriction.toPredicate( root, builder ) )
				.toList() );
	}
}
