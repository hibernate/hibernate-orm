/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.DeleteSpecification;

/**
 * @author Gavin King
 */
public class DeleteSpecificationImpl<T>
		extends MutationSpecificationImpl<T>
		implements DeleteSpecification<T> {

	public DeleteSpecificationImpl(@Nonnull Class<T> mutationTarget) {
		super( MutationType.DELETE, mutationTarget );
	}

	public DeleteSpecificationImpl(@Nonnull CriteriaDelete<T> criteriaQuery) {
		super( criteriaQuery );
	}

	@Nonnull
	@Override
	public DeleteSpecification<T> restrict(@Nonnull Restriction<? super T> restriction) {
		super.restrict( restriction );
		return this;
	}

	@Nonnull
	@Override
	public DeleteSpecification<T> augment(@Nonnull Augmentation<T> augmentation) {
		super.augment( augmentation );
		return this;
	}

	@Nonnull
	@Override
	public DeleteSpecification<T> validate(@Nonnull CriteriaBuilder builder) {
		super.validate( builder );
		return this;
	}
}
