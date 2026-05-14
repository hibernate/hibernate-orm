/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.PluralExpression;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaPluralExpression<C, E> extends PluralExpression<C, E>, JpaExpression<C> {
	@Override
	JpaPredicate isEmpty();

	@Override
	JpaPredicate isNotEmpty();

	@Override
	JpaNumericExpression<Integer> size();

	@Override
	JpaPredicate contains(Expression<? extends E> elem);

	@Override
	JpaPredicate contains(E elem);

	@Override
	JpaPredicate notContains(Expression<? extends E> elem);

	@Override
	JpaPredicate notContains(E elem);
}
