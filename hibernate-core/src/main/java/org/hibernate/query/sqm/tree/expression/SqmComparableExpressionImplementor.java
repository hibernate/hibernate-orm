/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

/**
 * @author Steve Ebersole
 */
public interface SqmComparableExpressionImplementor<C extends Comparable<? super C>> extends SqmComparableExpression<C> {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Nonnull
	@Override
	default SqmComparableExpression<C> coalesce(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<C> coalesce(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<C> nullif(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<C> nullif(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	default SqmPredicate greaterThan(@Nonnull Expression<? extends C> y) {
		return nodeBuilder().greaterThan( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate greaterThan(C y) {
		return nodeBuilder().greaterThan( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends C> y) {
		return nodeBuilder().greaterThanOrEqualTo( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate greaterThanOrEqualTo(C y) {
		return nodeBuilder().greaterThanOrEqualTo( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lessThan(@Nonnull Expression<? extends C> y) {
		return nodeBuilder().lessThan( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lessThan(C y) {
		return nodeBuilder().lessThan( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends C> y) {
		return nodeBuilder().lessThanOrEqualTo( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lessThanOrEqualTo(C y) {
		return nodeBuilder().lessThanOrEqualTo( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate between(@Nonnull Expression<? extends C> x, @Nonnull Expression<? extends C> y) {
		return nodeBuilder().between( this, x, y );
	}

	@Nonnull
	@Override
	default SqmPredicate between(C x, C y) {
		return nodeBuilder().between( this, x, y );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<C> max() {
		final var expression = nodeBuilder().greatest( this );
		return expression instanceof SqmComparableExpression<C> comparableExpression
				? comparableExpression
				: new SqmComparableExpressionWrapper<>( expression );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<C> min() {
		final var expression = nodeBuilder().least( this );
		return expression instanceof SqmComparableExpression<C> comparableExpression
				? comparableExpression
				: new SqmComparableExpressionWrapper<>( expression );
	}

	@Nonnull
	@Override
	default SqmSortSpecification asc() {
		return nodeBuilder().asc( this );
	}

	@Nonnull
	@Override
	default SqmSortSpecification asc(@Nonnull Nulls nullPrecedence) {
		return nodeBuilder().asc( this, nullPrecedence );
	}

	@Nonnull
	@Override
	default SqmSortSpecification desc() {
		return nodeBuilder().desc( this );
	}

	@Nonnull
	@Override
	default SqmSortSpecification desc(@Nonnull Nulls nullPrecedence) {
		return nodeBuilder().desc( this, nullPrecedence );
	}
}
