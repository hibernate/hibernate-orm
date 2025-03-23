/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaUpdate<T> extends JpaManipulationCriteria<T>, CriteriaUpdate<T> {

	boolean isVersioned();

	JpaCriteriaUpdate<T> versioned();

	JpaCriteriaUpdate<T> versioned(boolean versioned);

	@Override
	JpaRoot<T> from(Class<T> entityClass);

	@Override
	JpaRoot<T> from(EntityType<T> entity);

	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(SingularAttribute<? super T, Y> attribute, X value);

	@Override
	<Y> JpaCriteriaUpdate<T> set( SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value);

	@Override
	<Y, X extends Y> JpaCriteriaUpdate<T> set(Path<Y> attribute, X value);

	@Override
	<Y> JpaCriteriaUpdate<T> set(Path<Y> attribute, Expression<? extends Y> value);

	@Override
	JpaCriteriaUpdate<T> set(String attributeName, Object value);

	@Override
	JpaRoot<T> getRoot();

	@Override
	JpaCriteriaUpdate<T> where(Expression<Boolean> restriction);

	@Override
	JpaCriteriaUpdate<T> where(Predicate... restrictions);
}
