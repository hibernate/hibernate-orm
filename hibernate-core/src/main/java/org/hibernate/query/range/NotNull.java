/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * A {@link Range} containing every value of the given type,
 * except the null value.
 *
 * @author Gavin King
 */
record NotNull<U>(@Nonnull Class<U> type) implements Range<U> {
	@Nonnull
	@Override
	public Class<U> getType() {
		return type;
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends U> path, @Nonnull CriteriaBuilder builder) {
		return path.isNotNull();
	}
}
