/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * The base for {@code json_exists} function nodes.
 * @since 7.0
 */
@Incubating
public interface JpaJsonExistsNode {
	/**
	 * Get the {@link ErrorBehavior} of this json exists expression.
	 *
	 * @return the error behavior
	 */
	ErrorBehavior getErrorBehavior();

	/**
	 * Sets the {@link ErrorBehavior#UNSPECIFIED} for this json exists expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonExistsNode unspecifiedOnError();
	/**
	 * Sets the {@link ErrorBehavior#ERROR} for this json exists expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonExistsNode errorOnError();
	/**
	 * Sets the {@link ErrorBehavior#TRUE} for this json exists expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonExistsNode trueOnError();
	/**
	 * Sets the {@link ErrorBehavior#FALSE} for this json exists expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonExistsNode falseOnError();

	/**
	 * The behavior of the json exists expression when a JSON processing error occurs.
	 */
	enum ErrorBehavior {
		/**
		 * SQL/JDBC error should be raised.
		 */
		ERROR,
		/**
		 * {@code true} should be returned on error.
		 */
		TRUE,
		/**
		 * {@code false} should be returned on error.
		 */
		FALSE,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
}
