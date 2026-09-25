package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.range.Range;

/**
 * Restricts a path from an entity to a given {@link Range}.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 *
 * @author Gavin King
 */
record PathRange<X, U>(@Nonnull Path<X, U> path, @Nonnull Range<? super U> range) implements Restriction<X> {
	@Nonnull
	@Override
	public Restriction<X> negated() {
		return new Negation<>( this );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Root<? extends X> root, @Nonnull CriteriaBuilder builder) {
		return range.toPredicate( path.path( root ), builder );
	}
}
