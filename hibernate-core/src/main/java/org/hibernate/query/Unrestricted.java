/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A null restriction.
 */
public class Unrestricted<T> implements Restriction<T> {
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
