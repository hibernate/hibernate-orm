/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

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

	@Override
	default SqmPredicate greaterThan(Expression<? extends C> y) {
		return nodeBuilder().greaterThan( this, y );
	}

	@Override
	default SqmPredicate greaterThan(C y) {
		return nodeBuilder().greaterThan( this, y );
	}

	@Override
	default SqmPredicate greaterThanOrEqualTo(Expression<? extends C> y) {
		return nodeBuilder().greaterThanOrEqualTo( this, y );
	}

	@Override
	default SqmPredicate greaterThanOrEqualTo(C y) {
		return nodeBuilder().greaterThanOrEqualTo( this, y );
	}

	@Override
	default SqmPredicate lessThan(Expression<? extends C> y) {
		return nodeBuilder().lessThan( this, y );
	}

	@Override
	default SqmPredicate lessThan(C y) {
		return nodeBuilder().lessThan( this, y );
	}

	@Override
	default SqmPredicate lessThanOrEqualTo(Expression<? extends C> y) {
		return nodeBuilder().lessThanOrEqualTo( this, y );
	}

	@Override
	default SqmPredicate lessThanOrEqualTo(C y) {
		return nodeBuilder().lessThanOrEqualTo( this, y );
	}

	@Override
	default SqmPredicate between(Expression<? extends C> x, Expression<? extends C> y) {
		return nodeBuilder().between( this, x, y );
	}

	@Override
	default SqmPredicate between(C x, C y) {
		return nodeBuilder().between( this, x, y );
	}

	@Override
	default SqmSortSpecification asc() {
		return nodeBuilder().asc( this );
	}

	@Override
	default SqmSortSpecification asc(Nulls nullPrecedence) {
		return nodeBuilder().asc( this, nullPrecedence );
	}

	@Override
	default SqmSortSpecification desc() {
		return nodeBuilder().desc( this );
	}

	@Override
	default SqmSortSpecification desc(Nulls nullPrecedence) {
		return nodeBuilder().desc( this, nullPrecedence );
	}
}
