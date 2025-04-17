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
 * Specialization of {@link QuerySpecification} for programmatic customization of
 * {@linkplain MutationQuery mutation queries}.
 * <p>
 * The method {@link #addRestriction(Restriction)} allows application of additional
 * {@linkplain Restriction filtering} to the mutated entity. The static factory
 * methods of {@link Restriction} are used to express filtering criteria of various
 * kinds.
 * <p>
 * Once all {@linkplain #addRestriction restrictions} are specified, call
 * {@linkplain #createQuery()} to obtain an {@linkplain SelectionQuery an
 * executable mutation query object}.
 *
 * @param <T> The entity type which is the target of the mutation.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
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
	@Override
	MutationQuery createQuery();
}
