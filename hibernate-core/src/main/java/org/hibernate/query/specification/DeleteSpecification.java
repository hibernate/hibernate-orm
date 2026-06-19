/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import org.hibernate.Incubating;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.internal.DeleteSpecificationImpl;

/**
 * Specialization of {@link MutationSpecification} for programmatic customization
 * of delete queries.
 *
 * @param <T> The entity type which is the target of the mutation.
 *
 * @author Gavin King
 *
 * @since 7.2
 */
@Incubating
public interface DeleteSpecification<T> extends MutationSpecification<T> {
	@Nonnull
	@Override
	DeleteSpecification<T> restrict(@Nonnull Restriction<? super T> restriction);

	@Nonnull
	@Override
	DeleteSpecification<T> augment(@Nonnull Augmentation<T> augmentation);

	@Nonnull
	@Override
	DeleteSpecification<T> validate(@Nonnull CriteriaBuilder builder);

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain org.hibernate.query.MutationQuery} which
	 * deletes the given entity type.
	 *
	 * @param targetEntityClass The target entity type
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	@Nonnull
	static <T> DeleteSpecification<T> create(@Nonnull Class<T> targetEntityClass) {
		return new DeleteSpecificationImpl<>( targetEntityClass );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain org.hibernate.query.MutationQuery} based
	 * on the given criteria delete, allowing the addition of
	 * {@linkplain #restrict restrictions}.
	 *
	 * @param criteriaDelete The criteria delete query
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	@Nonnull
	static <T> DeleteSpecification<T> create(@Nonnull CriteriaDelete<T> criteriaDelete) {
		return new DeleteSpecificationImpl<>( criteriaDelete );
	}
}
