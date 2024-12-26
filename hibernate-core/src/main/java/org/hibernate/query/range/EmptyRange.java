/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * A {@link Range} containing no values.
 */
record EmptyRange<U>(Class<U> type) implements Range<U> {
	@Override
	public Class<? extends U> getType() {
		return type;
	}

	@Override
	public Predicate toPredicate(Path<U> path, CriteriaBuilder builder) {
		return builder.disjunction();
	}
}
