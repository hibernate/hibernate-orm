/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.query.assignment.Assignment;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.UpdateSpecification;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import java.util.List;

/**
 * @author Gavin King
 */
public class UpdateSpecificationImpl<T>
		extends MutationSpecificationImpl<T>
		implements UpdateSpecification<T> {

	public UpdateSpecificationImpl(Class<T> mutationTarget) {
		super( MutationType.UPDATE, mutationTarget );
	}

	public UpdateSpecificationImpl(CriteriaUpdate<T> criteriaQuery) {
		super( criteriaQuery );
	}

	@Override
	public UpdateSpecification<T> assign(Assignment<? super T> assignment) {
		specifications.add( (sqmStatement, mutationTargetRoot) -> {
			if ( sqmStatement instanceof SqmUpdateStatement<T> sqmUpdateStatement ) {
				assignment.apply( sqmUpdateStatement );
			}
			else {
				throw new IllegalStateException( "Delete query cannot perform assignment" );
			}
		} );
		return this;
	}

	@Override
	public UpdateSpecification<T> reassign(List<? extends Assignment<? super T>> assignments) {
		specifications.add( (sqmStatement, mutationTargetRoot) -> {
			if ( sqmStatement instanceof SqmUpdateStatement<T> sqmUpdateStatement ) {
				final var setClause = sqmUpdateStatement.getSetClause();
				if ( setClause != null ) {
					setClause.clearAssignments();
				}
				assignments.forEach( assignment -> assignment.apply( sqmUpdateStatement ) );
			}
			else {
				throw new IllegalStateException( "Delete query cannot perform assignment" );
			}
		} );
		return this;
	}

	@Override
	public UpdateSpecification<T> restrict(Restriction<? super T> restriction) {
		super.restrict( restriction );
		return this;
	}

	@Override
	public UpdateSpecification<T> augment(Augmentation<T> augmentation) {
		super.augment( augmentation );
		return this;
	}

	@Override
	public UpdateSpecification<T> validate(CriteriaBuilder builder) {
		super.validate( builder );
		return this;
	}
}
