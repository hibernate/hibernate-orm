/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.Incubating;
import org.hibernate.query.assignment.Assignment;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.internal.UpdateSpecificationImpl;

import java.util.List;

/**
 * Specialization of {@link MutationSpecification} for programmatic customization
 * of update queries.
 * <p>
 * The method {@link #assign(Assignment)} allows application of additional
 * {@linkplain Assignment assignments} to the mutated entity. The static factory
 * methods of {@link Assignment} are used to express assignments to attributes
 * or compound paths.
 * <pre>
 * UpdateSpecification.create(Book.class)
 *         .assign(Assignment.set(Book_.title, newTitle))
 *         .restrict(Restriction.equal(Book_.isbn, isbn))
 *         .createQuery(session)
 *         .executeUpdate();
 * </pre>
 *
 * @param <T> The entity type which is the target of the mutation.
 *
 * @author Gavin King
 *
 * @since 7.2
 */
@Incubating
public interface UpdateSpecification<T> extends MutationSpecification<T> {
	/**
	 * Add an assigment to a field or property of the target entity.
	 *
	 * @param assignment The assignment to add
	 *
	 * @return {@code this} for method chaining.
	 */
	UpdateSpecification<T> assign(Assignment<? super T> assignment);

	/**
	 * Sets the assignments to fields or properties of the target entity.
	 * If assignments were already specified, this method drops the previous
	 * assignments in favor of the passed {@code assignments}.
	 *
	 * @param assignments The new assignments
	 *
	 * @return {@code this} for method chaining.
	 */
	UpdateSpecification<T> reassign(List<? extends Assignment<? super T>> assignments);

	@Override
	UpdateSpecification<T> restrict(Restriction<? super T> restriction);

	@Override
	UpdateSpecification<T> augment(Augmentation<T> augmentation);

	@Override
	UpdateSpecification<T> validate(CriteriaBuilder builder);

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain org.hibernate.query.MutationQuery} which
	 * updates the given entity type.
	 *
	 * @param targetEntityClass The target entity type
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	static <T> UpdateSpecification<T> create(Class<T> targetEntityClass) {
		return new UpdateSpecificationImpl<>( targetEntityClass );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain org.hibernate.query.MutationQuery} based
	 * on the given criteria update, allowing the addition of
	 * {@linkplain #restrict restrictions} and {@linkplain #assign assignments}.
	 *
	 * @param criteriaUpdate The criteria update query
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	static <T> UpdateSpecification<T> create(CriteriaUpdate<T> criteriaUpdate) {
		return new UpdateSpecificationImpl<>( criteriaUpdate );
	}

}
