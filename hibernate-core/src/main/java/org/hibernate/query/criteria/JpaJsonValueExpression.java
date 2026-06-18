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

	/**
	 * Use the unspecified JSON error behavior.
	 */
	@Override
	JpaJsonValueExpression<T> unspecifiedOnError();
	/**
	 * Use the JSON error behavior that raises an error.
	 */
	@Override
	JpaJsonValueExpression<T> errorOnError();
	/**
	 * Use the JSON error behavior that returns null.
	 */
	@Override
	JpaJsonValueExpression<T> nullOnError();
	/**
	 * Use the JSON error behavior that returns the given default value.
	 */
	@Override
	JpaJsonValueExpression<T> defaultOnError(Expression<?> expression);

	/**
	 * Use the unspecified JSON empty behavior.
	 */
	@Override
	JpaJsonValueExpression<T> unspecifiedOnEmpty();
	/**
	 * Use the JSON empty behavior that raises an error.
	 */
	@Override
	JpaJsonValueExpression<T> errorOnEmpty();
	/**
	 * Use the JSON empty behavior that returns null.
	 */
	@Override
	JpaJsonValueExpression<T> nullOnEmpty();
	/**
	 * Use the JSON empty behavior that returns the given default value.
	 */
	@Override
	JpaJsonValueExpression<T> defaultOnEmpty(Expression<?> expression);

}
