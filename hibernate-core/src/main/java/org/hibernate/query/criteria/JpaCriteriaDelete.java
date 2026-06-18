/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaDelete<T> extends JpaManipulationCriteria<T>, CriteriaDelete<T> {

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
	JpaCriteriaDelete<T> where(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaDelete<T> where(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaDelete<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);
}
