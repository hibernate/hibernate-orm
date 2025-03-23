/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A conflict clause for insert statements.
 *
 * @since 6.5
 */
@Incubating
public interface JpaConflictClause<T> {

	/**
	 * The excluded row/object which was not inserted.
	 */
	JpaRoot<T> getExcludedRoot();

	/**
	 * The unique constraint name for which a constraint violation is allowed.
	 */
	@Nullable String getConstraintName();

	/**
	 * Sets the unique constraint name for which a constraint violation is allowed.
	 *
	 * @throws IllegalStateException when constraint paths have already been defined
	 */
	JpaConflictClause<T> conflictOnConstraint(@Nullable String constraintName);

	/**
	 * The paths which are part of a unique constraint, for which a constraint violation is allowed.
	 */
	List<? extends JpaPath<?>> getConstraintPaths();

	/**
	 * Shorthand for calling {@link #conflictOnConstraintPaths(List)} with paths resolved for the given attributes
	 * against the insert target.
	 */
	JpaConflictClause<T> conflictOnConstraintAttributes(String... attributes);

	/**
	 * Shorthand for calling {@link #conflictOnConstraintPaths(List)} with paths resolved for the given attributes
	 * against the insert target.
	 */
	JpaConflictClause<T> conflictOnConstraintAttributes(SingularAttribute<T, ?>... attributes);

	/**
	 * See {@link #conflictOnConstraintPaths(List)}.
	 */
	JpaConflictClause<T> conflictOnConstraintPaths(Path<?>... paths);

	/**
	 * Sets the paths which are part of a unique constraint, for which a constraint violation is allowed.
	 *
	 * @throws IllegalStateException when a constraint name has already been defined
	 */
	JpaConflictClause<T> conflictOnConstraintPaths(List<? extends Path<?>> paths);

	/**
	 * The action to do when a conflict due to a unique constraint violation happens.
	 */
	@Nullable JpaConflictUpdateAction<T> getConflictAction();

	/**
	 * Sets the action to do on a conflict. Setting {@code null} means to do nothing.
	 *
	 * @see #createConflictUpdateAction()
	 */
	JpaConflictClause<T> onConflictDo(@Nullable JpaConflictUpdateAction<T> action);

	/**
	 * Shorthand version for calling {@link #onConflictDo(JpaConflictUpdateAction)} with {@link #createConflictUpdateAction()}
	 * as argument and returning the update action.
	 */
	default JpaConflictUpdateAction<T> onConflictDoUpdate() {
		final JpaConflictUpdateAction<T> conflictUpdateAction = createConflictUpdateAction();
		onConflictDo( conflictUpdateAction );
		return conflictUpdateAction;
	}

	/**
	 * Shorthand version for calling {@link #onConflictDo(JpaConflictUpdateAction)} with a {@code null} argument.
	 */
	default JpaConflictClause<T> onConflictDoNothing() {
		return onConflictDo( null );
	}

	/**
	 * Create a new conflict update action for this insert statement.
	 *
	 * @return a new conflict update action
	 * @see #onConflictDo(JpaConflictUpdateAction)
	 */
	JpaConflictUpdateAction<T> createConflictUpdateAction();
}
