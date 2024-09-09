/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_query} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonQueryExpression extends JpaExpression<String> {
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
	JpaJsonQueryExpression withoutWrapper();
	/**
	 * Sets the {@link WrapMode#WITH_WRAPPER} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression withWrapper();
	/**
	 * Sets the {@link WrapMode#WITH_CONDITIONAL_WRAPPER} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression withConditionalWrapper();
	/**
	 * Sets the {@link WrapMode#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression unspecifiedWrapper();

	/**
	 * Sets the {@link ErrorBehavior#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression unspecifiedOnError();
	/**
	 * Sets the {@link ErrorBehavior#ERROR} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression errorOnError();
	/**
	 * Sets the {@link ErrorBehavior#NULL} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression nullOnError();
	/**
	 * Sets the {@link ErrorBehavior#EMPTY_ARRAY} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression emptyArrayOnError();
	/**
	 * Sets the {@link ErrorBehavior#EMPTY_OBJECT} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression emptyObjectOnError();

	/**
	 * Sets the {@link EmptyBehavior#UNSPECIFIED} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression unspecifiedOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#ERROR} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression errorOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#NULL} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression nullOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#EMPTY_ARRAY} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression emptyArrayOnEmpty();
	/**
	 * Sets the {@link EmptyBehavior#EMPTY_OBJECT} for this json query expression.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression emptyObjectOnEmpty();

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression passing(String parameterName, Expression<?> expression);

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
