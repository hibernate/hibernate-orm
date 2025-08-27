/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;

/**
 * A root element of a {@link Path}.
 *
 * @author Gavin King
 */
record PathRoot<X>(Class<X> type) implements Path<X, X> {
	@Override
	public Class<X> getType() {
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public jakarta.persistence.criteria.Path<X> path(Root<? extends X> root) {
		// unchecked cast only to get rid of the upper bound
		return (jakarta.persistence.criteria.Path<X>) root;
	}

	@Override
	public FetchParent<?, ? extends X> fetch(Root<? extends X> root) {
		return root;
	}
}
