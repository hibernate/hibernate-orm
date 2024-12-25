/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.List;

/**
 * Restricts to a list of literal values.
 */
record ValueList<U>(List<U> values) implements Range<U> {
	@Override
	public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
		return root.get( attribute ).in( values.stream().map( builder::literal ).toList() );
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends U> getType() {
		return (Class<? extends U>) values.get(0).getClass();
	}
}
