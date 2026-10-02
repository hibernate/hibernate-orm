package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * A {@link Range} containing no values.
 *
 * @author Gavin King
 */
record EmptyRange<U>(@Nonnull Class<U> type) implements Range<U> {
	@Nonnull
	@Override
	public Class<U> getType() {
		return type;
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends U> path, @Nonnull CriteriaBuilder builder) {
		return builder.disjunction();
	}
}
