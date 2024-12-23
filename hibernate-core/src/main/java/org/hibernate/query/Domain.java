/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.Internal;

import java.util.Locale;

/**
 * Specifies an allowed set of range of values for a value being restricted.
 *
 * @param <U> The type of the value being restricted
 *
 * @see Restriction
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating
interface Domain<U> {

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * attribute of the given root entity to this domain of allowed
	 * values.
	 */
	@Internal
	<X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X,U> attribute, CriteriaBuilder builder);

	/**
	 * Restricts to a single literal value.
	 */
	record Value<U>(U value) implements Domain<U> {
		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
			// TODO: it would be much better to not do use literal,
			//       and let it be treated as a parameter, but we
			//       we run into the usual bug with parameters in
			//       manipulated SQM trees
			final Expression<U> literal = builder.literal( value );
			return root.get(attribute).equalTo( literal );
		}
	}

	/**
	 * Restricts to a list of literal values.
	 */
	record List<U>(java.util.List<U> values) implements Domain<U> {
		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
			return root.get(attribute).in(values.stream().map(builder::literal).toList());
		}
	}

	/**
	 * Restricts a string by a pattern.
	 */
	record Pattern(String pattern, boolean caseSensitive) implements Domain<String> {
		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, String> attribute, CriteriaBuilder builder) {
			return caseSensitive
					? builder.like( root.get(attribute), builder.literal(pattern) )
					: builder.like( builder.lower(root.get(attribute)), builder.literal(pattern.toLowerCase(Locale.ROOT)) );
		}
	}

	/**
	 * Restricts to all values higher than a given lower bound.
	 */
	record LowerBound<U extends Comparable<U>>(U bound, boolean open) implements Domain<U> {
		static <U extends Comparable<U>> LowerBound<U> greaterThan(U bound) {
			return new LowerBound<>(bound, true);
		}
		static <U extends Comparable<U>> LowerBound<U> greaterThanOrEqualTo(U bound) {
			return new LowerBound<>(bound, false);
		}

		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
			// TODO: it would be much better to not do use literal,
			//       and let it be treated as a parameter, but we
			//       we run into the usual bug with parameters in
			//       manipulated SQM trees
			final Expression<U> literal = builder.literal( bound );
			return open
					? builder.greaterThan( root.get(attribute), literal )
					: builder.greaterThanOrEqualTo( root.get(attribute), literal );
		}
	}

	/**
	 * Restricts to all values lower than a given upper bound.
	 */
	record UpperBound<U extends Comparable<U>>(U bound, boolean open) implements Domain<U> {
		static <U extends Comparable<U>> UpperBound<U> lessThan(U bound) {
			return new UpperBound<>(bound, true);
		}
		static <U extends Comparable<U>> UpperBound<U> lessThanOrEqualTo(U bound) {
			return new UpperBound<>(bound, false);
		}

		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
			// TODO: it would be much better to not do use literal,
			//       and let it be treated as a parameter, but we
			//       we run into the usual bug with parameters in
			//       manipulated SQM trees
			final Expression<U> literal = builder.literal( bound );
			return open
					? builder.lessThan( root.get(attribute), literal )
					: builder.lessThanOrEqualTo( root.get(attribute), literal );
		}
	}

	/**
	 * Restricts to an upper-bounded and lower-bounded interval.
	 */
	record Interval<U extends Comparable<U>>(LowerBound<U> lowerBound, UpperBound<U> upperBound)
			implements Domain<U> {
		static <U extends Comparable<U>> Interval<U> open(U lowerBound, U upperBound) {
			return new Interval<>( new LowerBound<>(lowerBound, true),
					new UpperBound<>(upperBound, true) );
		}
		static <U extends Comparable<U>> Interval<U> closed(U lowerBound, U upperBound) {
			return new Interval<>( new LowerBound<>(lowerBound, false),
					new UpperBound<>(upperBound, false) );
		}

		@Override
		public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
			return lowerBound.open || upperBound.open
					? builder.and( lowerBound.toPredicate( root, attribute, builder ),
							upperBound.toPredicate( root, attribute, builder ) )
					: builder.between( root.get(attribute),
							builder.literal(lowerBound.bound), builder.literal(upperBound.bound) );
		}
	}

	/**
	 * Restricts an attribute of an entity to a given {@link Domain}.
	 *
	 * @param <X> The entity type
	 * @param <U> The attribute type
	 */
	class AttributeRestriction<X,U> implements Restriction<X> {
		private final SingularAttribute<X, U> attribute;
		private final Domain<U> domain;

		AttributeRestriction(SingularAttribute<X, U> attribute, Domain<U> domain) {
			this.attribute = attribute;
			this.domain = domain;
		}

		public Restriction<X> negated() {
			return new Negated<>(this);
		}

		@Override
		public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
			return domain.toPredicate( root, attribute, builder );
		}
	}

	/**
	 * Negates a restriction; a logical NOT.
	 *
	 * @param restriction The restriction to be negated
	 * @param <X> The entity type
	 */
	record Negated<X>(Restriction<X> restriction) implements Restriction<X> {
		@Override
		public Restriction<X> negated() {
			return restriction;
		}

		@Override
		public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
			return builder.not(restriction.toPredicate(root, builder));
		}
	}

	/**
	 * A compound restriction constructed using logical AND.
	 *
	 * @param restrictions The restrictions to be AND-ed
	 * @param <X> The entity type
	 */
	record Conjunction<X>(java.util.List<? extends Restriction<? super X>> restrictions)
			implements Restriction<X> {
		@Override
		public Restriction<X> negated() {
			return new Disjunction<>( restrictions.stream().map(Restriction::negated).toList() );
		}

		@Override
		public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
			return builder.and( restrictions.stream()
					.map( restriction -> restriction.toPredicate(root, builder) )
					.toList() );
		}
	}

	/**
	 * A compound restriction constructed using logical OR.
	 *
	 * @param restrictions The restrictions to be OR-ed
	 * @param <X> The entity type
	 */
	record Disjunction<X>(java.util.List<? extends Restriction<? super X>> restrictions)
			implements Restriction<X> {
		@Override
		public Restriction<X> negated() {
			return new Conjunction<>( restrictions.stream().map( Restriction::negated ).toList() );
		}

		@Override
		public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
			return builder.or( restrictions.stream()
					.map( restriction -> restriction.toPredicate(root, builder) )
					.toList() );
		}
	}
}
