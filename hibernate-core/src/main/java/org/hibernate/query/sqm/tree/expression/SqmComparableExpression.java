/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import org.hibernate.query.criteria.JpaComparableExpression;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

/**
 * @author Steve Ebersole
 */
public interface SqmComparableExpression<C extends Comparable<? super C>>
		extends JpaComparableExpression<C>, SqmExpression<C> {
	@Override
	SqmComparableExpression<C> coalesce(C y);

	@Override
	SqmComparableExpression<C> coalesce(Expression<? extends C> y);

	@Override
	SqmComparableExpression<C> nullif(C y);

	@Override
	SqmComparableExpression<C> nullif(Expression<? extends C> y);

	@Override
	SqmPredicate greaterThan(Expression<? extends C> y);

	@Override
	SqmPredicate greaterThan(C y);

	@Override
	SqmPredicate greaterThanOrEqualTo(Expression<? extends C> y);

	@Override
	SqmPredicate greaterThanOrEqualTo(C y);

	@Override
	SqmPredicate lessThan(Expression<? extends C> y);

	@Override
	SqmPredicate lessThan(C y);

	@Override
	SqmPredicate lessThanOrEqualTo(Expression<? extends C> y);

	@Override
	SqmPredicate lessThanOrEqualTo(C y);

	@Override
	SqmPredicate between(Expression<? extends C> x, Expression<? extends C> y);

	@Override
	SqmPredicate between(C x, C y);

	@Override
	SqmComparableExpression<C> max();

	@Override
	SqmComparableExpression<C> min();

	@Override
	SqmSortSpecification asc();

	@Override
	SqmSortSpecification asc(Nulls nullPrecedence);

	@Override
	SqmSortSpecification desc();

	@Override
	SqmSortSpecification desc(Nulls nullPrecedence);
}
