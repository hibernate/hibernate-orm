/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_value} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonValueExpression<T> extends JpaExpression<T>, JpaJsonValueNode<T> {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> passing(String parameterName, Expression<?> expression);

	@Override
	JpaJsonValueExpression<T> unspecifiedOnError();
	@Override
	JpaJsonValueExpression<T> errorOnError();
	@Override
	JpaJsonValueExpression<T> nullOnError();
	@Override
	JpaJsonValueExpression<T> defaultOnError(Expression<?> expression);

	@Override
	JpaJsonValueExpression<T> unspecifiedOnEmpty();
	@Override
	JpaJsonValueExpression<T> errorOnEmpty();
	@Override
	JpaJsonValueExpression<T> nullOnEmpty();
	@Override
	JpaJsonValueExpression<T> defaultOnEmpty(Expression<?> expression);

}
