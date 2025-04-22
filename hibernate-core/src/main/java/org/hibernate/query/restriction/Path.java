/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

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
@Incubating
public interface Path<X,U> {
	jakarta.persistence.criteria.Path<U> path(Root<? extends X> root);

	Class<U> getType();

	default <V> Path<X, V> to(SingularAttribute<? super U, V> attribute) {
		return new PathElement<>( this, attribute );
	}

	default <V> Path<X, V> to(String attributeName, Class<V> attributeType) {
		return new NamedPathElement<>( this, attributeName, attributeType );
	}

	static <X> Path<X, X> from(Class<X> type) {
		return new PathRoot<>( type );
	}

	default Restriction<X> restrict(Range<? super U> range) {
		return new PathRange<>( this, range );
	}

	default Restriction<X> equalTo(U value) {
		return restrict( Range.singleValue( value ) );
	}

	default Restriction<X> notEqualTo(U value) {
		return equalTo( value ).negated();
	}

	default Restriction<X> in(List<U> values) {
		return restrict( Range.valueList( values ) );
	}

	default Restriction<X> notIn(List<U> values) {
		return in( values ).negated();
	}

	default Restriction<X> notNull() {
		return restrict( Range.notNull( getType() ) );
	}

	FetchParent<?, ? extends U> fetch(Root<? extends X> root);
}
