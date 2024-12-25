/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Restricts to a single literal value.
 */
record Value<U>(U value) implements Range<U> {
	@Override
	public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		final Expression<U> literal = builder.literal( value );
		return root.get( attribute ).equalTo( literal );
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends U> getType() {
		return (Class<? extends U>) value.getClass();
	}
}
