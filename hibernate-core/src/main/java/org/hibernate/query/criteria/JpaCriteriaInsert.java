/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;

import jakarta.persistence.criteria.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The commonalities between insert-select and insert-values.
 *
 * @since 6.5
 */
@Incubating
public interface JpaCriteriaInsert<T> extends JpaManipulationCriteria<T> {

	/**
	 * Returns the insertion target paths.
	 */
	@Nonnull
	List<? extends JpaPath<?>> getInsertionTargetPaths();

	/**
	 * Sets the insertion target paths.
	 */
	@Nonnull
	JpaCriteriaInsert<T> setInsertionTargetPaths(@Nonnull Path<?>... insertionTargetPaths);

	/**
	 * Sets the insertion target paths.
	 */
	@Nonnull
	JpaCriteriaInsert<T> setInsertionTargetPaths(@Nonnull List<? extends Path<?>> insertionTargetPaths);

	/**
	 * Sets the conflict clause that defines what happens when an insert violates a unique constraint.
	 */
	@Nonnull
	JpaConflictClause<T> onConflict();

	/**
	 * Sets the conflict clause that defines what happens when an insert violates a unique constraint.
	 */
	@Nonnull
	JpaCriteriaInsert<T> onConflict(@Nullable JpaConflictClause<T> conflictClause);

	/**
	 * Returns the conflict clause that defines what happens when an insert violates a unique constraint,
	 * or {@code null} if there is none.
	 */
	@Nullable
	JpaConflictClause<T> getConflictClause();

	/**
	 * Create a new conflict clause for this insert statement.
	 *
	 * @return a new conflict clause
	 * @see JpaCriteriaInsert#onConflict(JpaConflictClause)
	 */
	@Nonnull
	JpaConflictClause<T> createConflictClause();
}
