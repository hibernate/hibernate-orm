/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * The update action that should happen on a unique constraint violation for an insert statement.
 *
 * @since 6.5
 */
@Incubating
public interface JpaConflictUpdateAction<T> {

	/**
	 * Update the value of the specified attribute.
	 * @param attribute  attribute to be updated
	 * @param value  new value
	 * @return  the modified update query
	 */
	@Nonnull
	<Y, X extends Y> JpaConflictUpdateAction<T> set(@Nonnull SingularAttribute<? super T, Y> attribute, @Nullable X value);

	/**
	 * Update the value of the specified attribute.
	 * @param attribute  attribute to be updated
	 * @param value  new value
	 * @return  the modified update query
	 */
	@Nonnull
	<Y> JpaConflictUpdateAction<T> set(@Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull Expression<? extends Y> value);

	/**
	 * Update the value of the specified attribute.
	 * @param attribute  attribute to be updated
	 * @param value  new value
	 * @return  the modified update query
	 */
	@Nonnull
	<Y, X extends Y> JpaConflictUpdateAction<T> set(@Nonnull Path<Y> attribute, @Nullable X value);

	/**
	 * Update the value of the specified attribute.
	 * @param attribute  attribute to be updated
	 * @param value  new value
	 * @return  the modified update query
	 */
	@Nonnull
	<Y> JpaConflictUpdateAction<T> set(@Nonnull Path<Y> attribute, @Nonnull Expression<? extends Y> value);

	/**
	 * Update the value of the specified attribute.
	 * @param attributeName  name of the attribute to be updated
	 * @param value  new value
	 * @return  the modified update query
	 */
	@Nonnull
	JpaConflictUpdateAction<T> set(@Nonnull String attributeName, @Nullable Object value);

	/**
	 * Modify the update query to restrict the target of the update
	 * according to the specified boolean expression.
	 * Replaces the previously added restriction(s), if any.
	 * @param restriction  a simple or compound boolean expression
	 * @return the modified update query
	 */
	@Nonnull
	JpaConflictUpdateAction<T> where(@Nullable Expression<Boolean> restriction);

	/**
	 * Modify the update query to restrict the target of the update
	 * according to the conjunction of the specified restriction
	 * predicates.
	 * Replaces the previously added restriction(s), if any.
	 * If no restrictions are specified, any previously added
	 * restrictions are simply removed.
	 * @param restrictions  zero or more restriction predicates
	 * @return the modified update query
	 */
	@Nonnull
	JpaConflictUpdateAction<T> where(@Nonnull Predicate... restrictions);

	/**
	 * Return the predicate that corresponds to the where clause
	 * restriction(s), or null if no restrictions have been
	 * specified.
	 * @return where clause predicate
	 */
	@Nullable JpaPredicate getRestriction();
}
