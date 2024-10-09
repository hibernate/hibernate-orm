/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

/**
 * Commonality between a JPA {@link JpaCriteriaQuery} and {@link JpaSubQuery},
 * mainly in the form of delegation to {@link JpaQueryStructure}
 *
 * @author Steve Ebersole
 */
public interface JpaSelectCriteria<T> extends AbstractQuery<T>, JpaCriteriaBase {
	/**
	 * The query structure.  See {@link JpaQueryStructure} for details
	 */
	JpaQueryStructure<T> getQuerySpec();
	/**
	 * The query structure.  See {@link JpaQueryStructure} for details
	 */
	JpaQueryPart<T> getQueryPart();

	/**
	 * Create and add a query root corresponding to the given subquery,
	 * forming a cartesian product with any existing roots.
	 *
	 * @param subquery the subquery
	 * @return query root corresponding to the given subquery
	 */
	<X> JpaDerivedRoot<X> from(Subquery<X> subquery);

	/**
	 * Create and add a query root corresponding to the given cte,
	 * forming a cartesian product with any existing roots.
	 *
	 * @param cte the cte criteria
	 * @return query root corresponding to the given cte
	 */
	<X> JpaRoot<X> from(JpaCteCriteria<X> cte);

	/**
	 * Create and add a query root corresponding to the given set-returning function,
	 * forming a cartesian product with any existing roots.
	 *
	 * @param function the set-returning function
	 * @return query root corresponding to the given function
	 */
	<X> JpaFunctionRoot<X> from(JpaSetReturningFunction<X> function);

	@Override
	JpaSelectCriteria<T> distinct(boolean distinct);

	@Override
	JpaSelection<T> getSelection();

	@Override
	<X> JpaRoot<X> from(Class<X> entityClass);

	@Override
	<X> JpaRoot<X> from(EntityType<X> entity);

	@Override
	JpaPredicate getRestriction();

	@Override
	JpaSelectCriteria<T> where(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> where(Predicate... restrictions);

	@Override
	JpaSelectCriteria<T> groupBy(Expression<?>... grouping);

	@Override
	JpaSelectCriteria<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaPredicate getGroupRestriction();

	@Override
	JpaSelectCriteria<T> having(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> having(Predicate... restrictions);
}
