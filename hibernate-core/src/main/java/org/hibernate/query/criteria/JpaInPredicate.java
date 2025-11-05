/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @author Steve Ebersole
 */
public interface JpaInPredicate<T> extends JpaPredicate, CriteriaBuilder.In<T>  {
	/**
	 * Return the expression to be tested against the
	 * list of values.
	 * @return expression
	 */
	@Override
	JpaExpression<T> getExpression();

	/**
	 *  Add to list of values to be tested against.
	 *  @param value value
	 *  @return in predicate
	 */
	@Override
	JpaInPredicate<T> value(@NonNull T value);

	/**
	 *  Add to list of values to be tested against.
	 *  @param value expression
	 *  @return in predicate
	 */
	JpaInPredicate<T> value(JpaExpression<? extends T> value);
}
