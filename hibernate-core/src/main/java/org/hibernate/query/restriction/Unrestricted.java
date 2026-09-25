package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A null restriction.
 */
record Unrestricted<T>() implements Restriction<T> {
	@Nonnull
	@Override
	public Restriction<T> negated() {
		return new Restriction<>() {
			@Nonnull
			@Override
			public Predicate toPredicate(@Nonnull Root<? extends T> root, @Nonnull CriteriaBuilder builder) {
				return builder.disjunction();
			}

			@Nonnull
			@Override
			public Restriction<T> negated() {
				return Unrestricted.this;
			}
		};
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Root<? extends T> root, @Nonnull CriteriaBuilder builder) {
		return builder.conjunction();
	}
}
