/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * A {@link Range} containing no values.
 *
 * @author Gavin King
 */
record EmptyRange<U>(Class<U> type) implements Range<U> {
	@Override
	public Class<U> getType() {
		return type;
	}

	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		return builder.disjunction();
	}
}
