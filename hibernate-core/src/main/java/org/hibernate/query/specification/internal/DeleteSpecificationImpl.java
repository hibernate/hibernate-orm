/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

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

	public DeleteSpecificationImpl(Class<T> mutationTarget) {
		super( MutationType.DELETE, mutationTarget );
	}

	public DeleteSpecificationImpl(CriteriaDelete<T> criteriaQuery) {
		super( criteriaQuery );
	}

@Override
	public DeleteSpecification<T> restrict(Restriction<? super T> restriction) {
		super.restrict( restriction );
		return this;
	}

	@Override
	public DeleteSpecification<T> augment(Augmentation<T> augmentation) {
		super.augment( augmentation );
		return this;
	}

	@Override
	public DeleteSpecification<T> validate(CriteriaBuilder builder) {
		super.validate( builder );
		return this;
	}
}
