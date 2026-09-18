/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.query.range.Range;

import java.util.List;

/**
 * Allows construction of a {@link Restriction} on a compound path.
 * <p>
 * A compound path is a sequence of attribute references rooted at
 * the root entity type of the query.
 * <pre>
 * SelectionSpecification.create(Book.class)
 *         .restrict(from(Book.class).to(Book_.publisher).to(Publisher_.name)
 *                         .equalTo("Manning"))
 *         .fetch(from(Book.class).to(Book_.publisher))
 *         .createQuery(session)
 *         .getResultList()
 * </pre>
 * A compound path-based restriction has the same semantics as the
 * equivalent implicit join in HQL.
 *
 * @param <X> The root entity type
 * @param <U> The leaf attribute type
 *
 * @see Restriction
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating(since = "7.0", group = "query-specifications")
public interface Path<X,U> {
	@Nonnull
	jakarta.persistence.criteria.Path<U> path(@Nonnull Root<? extends X> root);

	@Nonnull
	Class<U> getType();

	@Nonnull
	default <V> Path<X, V> to(@Nonnull SingularAttribute<? super U, V> attribute) {
		return new PathElement<>( this, attribute );
	}

	@Nonnull
	default <V> Path<X, V> to(@Nonnull String attributeName, @Nonnull Class<V> attributeType) {
		return new NamedPathElement<>( this, attributeName, attributeType );
	}

	@Nonnull
	static <X> Path<X, X> from(@Nonnull Class<X> type) {
		return new PathRoot<>( type );
	}

	@Nonnull
	default Restriction<X> restrict(@Nonnull Range<? super U> range) {
		return new PathRange<>( this, range );
	}

	@Nonnull
	default Restriction<X> equalTo(@Nonnull U value) {
		return restrict( Range.singleValue( value ) );
	}

	@Nonnull
	default Restriction<X> notEqualTo(@Nonnull U value) {
		return equalTo( value ).negated();
	}

	@Nonnull
	default Restriction<X> in(@Nonnull List<U> values) {
		return restrict( Range.valueList( values ) );
	}

	@Nonnull
	default Restriction<X> notIn(@Nonnull List<U> values) {
		return in( values ).negated();
	}

	@Nonnull
	default Restriction<X> notNull() {
		return restrict( Range.notNull( getType() ) );
	}

	@Nonnull
	FetchParent<?, ? extends U> fetch(@Nonnull Root<? extends X> root);
}
