/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
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
	@Nonnull
	@Override
	SqmComparableExpression<C> coalesce(C y);

	@Nonnull
	@Override
	SqmComparableExpression<C> coalesce(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmComparableExpression<C> nullif(C y);

	@Nonnull
	@Override
	SqmComparableExpression<C> nullif(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate greaterThan(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate greaterThan(C y);

	@Nonnull
	@Override
	SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate greaterThanOrEqualTo(C y);

	@Nonnull
	@Override
	SqmPredicate lessThan(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate lessThan(C y);

	@Nonnull
	@Override
	SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate lessThanOrEqualTo(C y);

	@Nonnull
	@Override
	SqmPredicate between(@Nonnull Expression<? extends C> x, @Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	SqmPredicate between(C x, C y);

	@Nonnull
	@Override
	SqmComparableExpression<C> max();

	@Nonnull
	@Override
	SqmComparableExpression<C> min();

	@Nonnull
	@Override
	SqmSortSpecification asc();

	@Nonnull
	@Override
	SqmSortSpecification asc(@Nonnull Nulls nullPrecedence);

	@Nonnull
	@Override
	SqmSortSpecification desc();

	@Nonnull
	@Override
	SqmSortSpecification desc(@Nonnull Nulls nullPrecedence);
}
