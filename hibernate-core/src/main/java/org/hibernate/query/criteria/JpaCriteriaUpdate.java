/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaUpdate<T> extends JpaManipulationCriteria<T>, CriteriaUpdate<T> {

	/**
	 * Return whether this update statement is versioned.
	 */
	boolean isVersioned();

	/**
	 * Mark this update statement as versioned.
	 */
	JpaCriteriaUpdate<T> versioned();

	/**
	 * Set whether this update statement is versioned.
	 */
	JpaCriteriaUpdate<T> versioned(boolean versioned);

	/**
	 * Create the root for the target entity.
	 */
	@Nonnull
	@Override
	JpaRoot<T> from(@Nonnull Class<T> entityClass);

	/**
	 * Create the root for the target entity.
	 */
	@Nonnull
	@Override
	JpaRoot<T> from(@Nonnull EntityType<T> entity);

	/**
	 * Set an attribute value for this update statement.
	 */
	@Nonnull
	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(@Nonnull SingularAttribute<? super T, Y> attribute, @Nullable X value);

	/**
	 * Set an attribute value for this update statement.
	 */
	@Nonnull
	@Override
	<Y> JpaCriteriaUpdate<T> set( @Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull Expression<? extends Y> value);

	/**
	 * Set an attribute value for this update statement.
	 */
	@Nonnull
	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(@Nonnull Path<Y> attribute, @Nullable X value);

	/**
	 * Set an attribute value for this update statement.
	 */
	@Nonnull
	@Override
	<Y> JpaCriteriaUpdate<T> set(@Nonnull Path<Y> attribute, @Nonnull Expression<? extends Y> value);

	/**
	 * Set an attribute value for this update statement.
	 */
	@Nonnull
	@Override
	JpaCriteriaUpdate<T> set(@Nonnull String attributeName, @Nullable Object value);

	/**
	 * Return the root of this criteria statement.
	 */
	@Nullable
	@Override
	JpaRoot<T> getRoot();

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);
}
