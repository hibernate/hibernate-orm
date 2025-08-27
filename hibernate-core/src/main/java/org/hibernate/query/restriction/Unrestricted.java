/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A null restriction.
 */
record Unrestricted<T>() implements Restriction<T> {
	@Override
	public Restriction<T> negated() {
		return new Restriction<>() {
			@Override
			public Predicate toPredicate(Root<? extends T> root, CriteriaBuilder builder) {
				return builder.disjunction();
			}

			@Override
			public Restriction<T> negated() {
				return Unrestricted.this;
			}
		};
	}

	@Override
	public Predicate toPredicate(Root<? extends T> root, CriteriaBuilder builder) {
		return builder.conjunction();
	}
}
