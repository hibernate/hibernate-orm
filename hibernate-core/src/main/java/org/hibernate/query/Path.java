/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Root;
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
 * session.createSelectionQuery("from Book", Book.class)
 *         .addRestriction(root(publisher).get(name).equalTo("Manning"))
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

	default <V> Path<X, V> get(SingularAttribute<? super U, V> attribute) {
		return new PathElement<>( this, attribute );
	}

	static <X, U> Path<X, U> root(SingularAttribute<? super X, U> attribute) {
		return new PathRoot<X>().get( attribute );
	}

	default Restriction<X> restrict(Range<U> range) {
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

	//TODO: between, lessThan, greaterThan?
}
