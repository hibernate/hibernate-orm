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

	boolean isVersioned();

	JpaCriteriaUpdate<T> versioned();

	JpaCriteriaUpdate<T> versioned(boolean versioned);

	@Nonnull
	@Override
	JpaRoot<T> from(@Nonnull Class<T> entityClass);

	@Nonnull
	@Override
	JpaRoot<T> from(@Nonnull EntityType<T> entity);

	@Nonnull
	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(@Nonnull SingularAttribute<? super T, Y> attribute, @Nullable X value);

	@Nonnull
	@Override
	<Y> JpaCriteriaUpdate<T> set( @Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull Expression<? extends Y> value);

	@Nonnull
	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(@Nonnull Path<Y> attribute, @Nullable X value);

	@Nonnull
	@Override
	<Y> JpaCriteriaUpdate<T> set(@Nonnull Path<Y> attribute, @Nonnull Expression<? extends Y> value);

	@Nonnull
	@Override
	JpaCriteriaUpdate<T> set(@Nonnull String attributeName, @Nullable Object value);

	@Nullable
	@Override
	JpaRoot<T> getRoot();

	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull Expression<Boolean> restriction);

	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaCriteriaUpdate<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);
}
