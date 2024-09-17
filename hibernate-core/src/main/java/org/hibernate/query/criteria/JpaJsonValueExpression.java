/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A special expression for the {@code json_value} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonValueExpression<T> extends JpaExpression<T> {
	/**
	 * Get the {@link ErrorBehavior} of this json value expression.
	 *
	 * @return the error behavior
	 */
	ErrorBehavior getErrorBehavior();

	/**
	 * Get the {@link EmptyBehavior} of this json value expression.
	 *
	 * @return the empty behavior
	 */
	EmptyBehavior getEmptyBehavior();

	/**
	 * Get the {@link JpaExpression} that is returned on a json processing error.
	 * Returns {@code null} if {@link #getErrorBehavior()} is not {@link ErrorBehavior#DEFAULT}.
	 *
	 * @return the value to return on a json processing error
	 */
	@Nullable JpaExpression<T> getErrorDefault();

	/**
	 * Get the {@link JpaExpression} that is returned when the JSON path does not resolve for a JSON document.
	 * Returns {@code null} if {@link #getEmptyBehavior()} is not {@link EmptyBehavior#DEFAULT}.
	 *
	 * @return the value to return on a json processing error
	 */
	@Nullable JpaExpression<T> getEmptyDefault();

	/**
	 * Sets the {@link ErrorBehavior#UNSPECIFIED} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> unspecifiedOnError();
	/**
	 * Sets the {@link ErrorBehavior#ERROR} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> errorOnError();
	/**
	 * Sets the {@link ErrorBehavior#NULL} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> nullOnError();
	/**
	 * Sets the {@link ErrorBehavior#DEFAULT} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> defaultOnError(Expression<?> expression);

	/**
	 * Sets the {@link EmptyBehavior#UNSPECIFIED} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> unspecifiedOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#ERROR} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> errorOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#NULL} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> nullOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#DEFAULT} for this json value expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> defaultOnEmpty(Expression<?> expression);

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonValueExpression<T> passing(String parameterName, Expression<?> expression);

	/**
	 * The behavior of the json value expression when a JSON processing error occurs.
	 */
	enum ErrorBehavior {
		/**
		 * SQL/JDBC error should be raised.
		 */
		ERROR,
		/**
		 * {@code null} should be returned.
		 */
		NULL,
		/**
		 * The {@link JpaJsonValueExpression#getErrorDefault()} value should be returned.
		 */
		DEFAULT,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
	/**
	 * The behavior of the json value expression when a JSON path does not resolve for a JSON document.
	 */
	enum EmptyBehavior {
		/**
		 * SQL/JDBC error should be raised.
		 */
		ERROR,
		/**
		 * {@code null} should be returned.
		 */
		NULL,
		/**
		 * The {@link JpaJsonValueExpression#getEmptyDefault()} value should be returned.
		 */
		DEFAULT,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
}
