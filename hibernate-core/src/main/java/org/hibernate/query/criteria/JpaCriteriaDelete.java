/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaDelete<T> extends JpaManipulationCriteria<T>, CriteriaDelete<T> {

	@Override
	JpaRoot<T> from(Class<T> entityClass);

	@Override
	JpaRoot<T> from(EntityType<T> entity);

	@Override
	JpaRoot<T> getRoot();

	@Override
	JpaCriteriaDelete<T> where(Expression<Boolean> restriction);

	@Override
	JpaCriteriaDelete<T> where(BooleanExpression... restrictions);

	@Override
	JpaCriteriaDelete<T> where(List<? extends Expression<Boolean>> restrictions);
}
