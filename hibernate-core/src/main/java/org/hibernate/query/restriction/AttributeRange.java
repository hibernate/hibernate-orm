package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.range.Range;

/**
 * Restricts an attribute of an entity to a given {@link Range}.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 *
 * @author Gavin King
 */
record AttributeRange<X, U>(@Nonnull SingularAttribute<X, U> attribute, @Nonnull Range<U> range) implements Restriction<X> {
	@Nonnull
	@Override
	public Restriction<X> negated() {
		return new Negation<>( this );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Root<? extends X> root, @Nonnull CriteriaBuilder builder) {
		return range.toPredicate( root.get( attribute ), builder );
	}
}
