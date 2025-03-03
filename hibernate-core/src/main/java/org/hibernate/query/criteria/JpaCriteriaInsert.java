/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

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
	List<? extends JpaPath<?>> getInsertionTargetPaths();

	/**
	 * Sets the insertion target paths.
	 */
	JpaCriteriaInsert<T> setInsertionTargetPaths(Path<?>... insertionTargetPaths);

	/**
	 * Sets the insertion target paths.
	 */
	JpaCriteriaInsert<T> setInsertionTargetPaths(List<? extends Path<?>> insertionTargetPaths);

	/**
	 * Sets the conflict clause that defines what happens when an insert violates a unique constraint.
	 */
	JpaConflictClause<T> onConflict();

	/**
	 * Sets the conflict clause that defines what happens when an insert violates a unique constraint.
	 */
	JpaCriteriaInsert<T> onConflict(@Nullable JpaConflictClause<T> conflictClause);

	/**
	 * Returns the conflict clause that defines what happens when an insert violates a unique constraint,
	 * or {@code null} if there is none.
	 */
	@Nullable JpaConflictClause<T> getConflictClause();

	/**
	 * Create a new conflict clause for this insert statement.
	 *
	 * @return a new conflict clause
	 * @see JpaCriteriaInsert#onConflict(JpaConflictClause)
	 */
	JpaConflictClause<T> createConflictClause();
}
