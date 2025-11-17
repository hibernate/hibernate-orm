/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * The base for {@code json_query} function nodes.
 * @since 7.0
 */
@Incubating
public interface JpaJsonQueryNode {
	/**
	 * Get the {@link WrapMode} of this json query expression.
	 *
	 * @return the wrap mode
	 */
	WrapMode getWrapMode();
	/**
	 * Get the {@link ErrorBehavior} of this json query expression.
	 *
	 * @return the error behavior
	 */
	ErrorBehavior getErrorBehavior();

	/**
	 * Get the {@link EmptyBehavior} of this json query expression.
	 *
	 * @return the empty behavior
	 */
	EmptyBehavior getEmptyBehavior();

	/**
	 * Sets the {@link WrapMode#WITHOUT_WRAPPER} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode withoutWrapper();
	/**
	 * Sets the {@link WrapMode#WITH_WRAPPER} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode withWrapper();
	/**
	 * Sets the {@link WrapMode#WITH_CONDITIONAL_WRAPPER} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode withConditionalWrapper();
	/**
	 * Sets the {@link WrapMode#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode unspecifiedWrapper();

	/**
	 * Sets the {@link ErrorBehavior#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode unspecifiedOnError();
	/**
	 * Sets the {@link ErrorBehavior#ERROR} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode errorOnError();
	/**
	 * Sets the {@link ErrorBehavior#NULL} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode nullOnError();
	/**
	 * Sets the {@link ErrorBehavior#EMPTY_ARRAY} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode emptyArrayOnError();
	/**
	 * Sets the {@link ErrorBehavior#EMPTY_OBJECT} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode emptyObjectOnError();

	/**
	 * Sets the {@link EmptyBehavior#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode unspecifiedOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#ERROR} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode errorOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#NULL} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode nullOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#EMPTY_ARRAY} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode emptyArrayOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#EMPTY_OBJECT} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryNode emptyObjectOnEmpty();

	/**
	 * The kind of wrapping to apply to the results of the query.
	 */
	enum WrapMode {
		/**
		 * Omit the array wrapper in the result.
		 */
		WITHOUT_WRAPPER,
		/**
		 * Force the array wrapper in the result.
		 */
		WITH_WRAPPER,
		/**
		 * Only use an array wrapper in the result if there is more than one result.
		 */
		WITH_CONDITIONAL_WRAPPER,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
	/**
	 * The behavior of the json query expression when a JSON processing error occurs.
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
		 * An empty array should be returned.
		 */
		EMPTY_ARRAY,
		/**
		 * An empty object should be returned.
		 */
		EMPTY_OBJECT,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
	/**
	 * The behavior of the json query expression when a JSON path does not resolve for a JSON document.
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
		 * An empty array should be returned.
		 */
		EMPTY_ARRAY,
		/**
		 * An empty object should be returned.
		 */
		EMPTY_OBJECT,
		/**
		 * Unspecified behavior i.e. the default database behavior.
		 */
		UNSPECIFIED
	}
}
