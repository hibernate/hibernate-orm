/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import jakarta.persistence.criteria.Root;
import org.hibernate.Incubating;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Restriction;

/**
 * Specialization of QuerySpecification for building
 * {@linkplain MutationQuery mutation queries}.
 * Once all {@linkplain #addRestriction restrictions} are defined, call
 * {@linkplain #createQuery()} to obtain the executable form.
 *
 * @param <T> The entity type which is the target of the mutation.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MutationSpecification<T> extends QuerySpecification<T> {
	/**
	 * The entity being mutated.
	 */
	default Root<T> getMutationTarget() {
		return getRoot();
	}

	/**
	 * Covariant override.
	 */
	@Override
	MutationSpecification<T> addRestriction(Restriction<T> restriction);

	/**
	 * Finalize the building and create the {@linkplain SelectionQuery} instance.
	 */
	MutationQuery createQuery();
}
