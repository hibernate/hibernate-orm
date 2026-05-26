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
	@Nonnull
	@Override
	JpaPredicate isEmpty();

	@Nonnull
	@Override
	JpaPredicate isNotEmpty();

	@Nonnull
	@Override
	JpaNumericExpression<Integer> size();

	@Nonnull
	@Override
	JpaPredicate contains(@Nonnull Expression<? extends E> elem);

	@Nonnull
	@Override
	JpaPredicate contains(@Nonnull E elem);

	@Nonnull
	@Override
	JpaPredicate notContains(@Nonnull Expression<? extends E> elem);

	@Nonnull
	@Override
	JpaPredicate notContains(@Nonnull E elem);
}
