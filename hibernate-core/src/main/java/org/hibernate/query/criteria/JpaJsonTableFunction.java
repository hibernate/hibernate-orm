/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Expression;
import org.hibernate.Incubating;

/**
 * A special expression for the {@code json_table} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonTableFunction extends JpaJsonTableColumnsNode {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonTableFunction passing(String parameterName, Expression<?> expression);

	/**
	 * Get the {@link ErrorBehavior} of this json table expression.
	 *
	 * @return the error behavior
	 */
	ErrorBehavior getErrorBehavior();

	/**
	 * Sets the {@link ErrorBehavior#UNSPECIFIED} for this json table expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonTableFunction unspecifiedOnError();
	/**
	 * Sets the {@link ErrorBehavior#ERROR} for this json table expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonTableFunction errorOnError();
	/**
	 * Sets the {@link ErrorBehavior#NULL} for this json table expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonTableFunction nullOnError();

	@Override
	JpaJsonTableFunction ordinalityColumn(String columnName);

	/**
	 * The behavior of the json exists expression when a JSON processing error occurs.
	 */
	enum ErrorBehavior {
		/**
		 * SQL/JDBC error should be raised.
		 */
		ERROR,
		/**
		 * {@code null} should be returned on error.
		 */
		NULL,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
}
