/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.PluralExpression;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaPluralExpression<C, E> extends PluralExpression<C, E>, JpaExpression<C> {
	/**
	 * Create a predicate testing whether this plural expression is empty.
	 */
	@Nonnull
	@Override
	JpaPredicate isEmpty();

	/**
	 * Create a predicate testing whether this plural expression is not empty.
	 */
	@Nonnull
	@Override
	JpaPredicate isNotEmpty();

	/**
	 * Create an expression for the size of this plural expression.
	 */
	@Nonnull
	@Override
	JpaNumericExpression<Integer> size();

	/**
	 * Create a predicate testing whether this plural expression contains the given element.
	 */
	@Nonnull
	@Override
	JpaPredicate contains(@Nonnull Expression<? extends E> elem);

	/**
	 * Create a predicate testing whether this plural expression contains the given element.
	 */
	@Nonnull
	@Override
	JpaPredicate contains(@Nonnull E elem);

	/**
	 * Create a predicate testing whether this plural expression does not contain the given element.
	 */
	@Nonnull
	@Override
	JpaPredicate notContains(@Nonnull Expression<? extends E> elem);

	/**
	 * Create a predicate testing whether this plural expression does not contain the given element.
	 */
	@Nonnull
	@Override
	JpaPredicate notContains(@Nonnull E elem);
}
