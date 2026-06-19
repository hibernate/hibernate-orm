/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.query.assignment.Assignment;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.UpdateSpecification;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

import java.util.List;

/**
 * @author Gavin King
 */
public class UpdateSpecificationImpl<T>
		extends MutationSpecificationImpl<T>
		implements UpdateSpecification<T> {

	public UpdateSpecificationImpl(@Nonnull Class<T> mutationTarget) {
		super( MutationType.UPDATE, mutationTarget );
	}

	public UpdateSpecificationImpl(@Nonnull CriteriaUpdate<T> criteriaQuery) {
		super( criteriaQuery );
	}

	@Nonnull
	@Override
	public UpdateSpecification<T> assign(@Nonnull Assignment<? super T> assignment) {
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

	@Nonnull
	@Override
	public UpdateSpecification<T> reassign(@Nonnull List<? extends Assignment<? super T>> assignments) {
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

	@Nonnull
	@Override
	public UpdateSpecification<T> restrict(@Nonnull Restriction<? super T> restriction) {
		super.restrict( restriction );
		return this;
	}

	@Nonnull
	@Override
	public UpdateSpecification<T> augment(@Nonnull Augmentation<T> augmentation) {
		super.augment( augmentation );
		return this;
	}

	@Nonnull
	@Override
	public UpdateSpecification<T> validate(@Nonnull CriteriaBuilder builder) {
		super.validate( builder );
		return this;
	}
}
