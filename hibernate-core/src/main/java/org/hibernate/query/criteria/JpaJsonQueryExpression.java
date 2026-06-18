/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_query} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonQueryExpression extends JpaExpression<String>, JpaJsonQueryNode {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonQueryExpression passing(String parameterName, Expression<?> expression);

	/**
	 * Use the JSON query wrapper mode without a wrapper.
	 */
	@Override
	JpaJsonQueryExpression withoutWrapper();
	/**
	 * Use the JSON query wrapper mode with a wrapper.
	 */
	@Override
	JpaJsonQueryExpression withWrapper();
	/**
	 * Use the JSON query wrapper mode with a conditional wrapper.
	 */
	@Override
	JpaJsonQueryExpression withConditionalWrapper();
	/**
	 * Use the unspecified JSON query wrapper mode.
	 */
	@Override
	JpaJsonQueryExpression unspecifiedWrapper();

	/**
	 * Use the unspecified JSON error behavior.
	 */
	@Override
	JpaJsonQueryExpression unspecifiedOnError();
	/**
	 * Use the JSON error behavior that raises an error.
	 */
	@Override
	JpaJsonQueryExpression errorOnError();
	/**
	 * Use the JSON error behavior that returns null.
	 */
	@Override
	JpaJsonQueryExpression nullOnError();
	/**
	 * Use the JSON error behavior that returns an empty array.
	 */
	@Override
	JpaJsonQueryExpression emptyArrayOnError();
	/**
	 * Use the JSON error behavior that returns an empty object.
	 */
	@Override
	JpaJsonQueryExpression emptyObjectOnError();

	/**
	 * Use the unspecified JSON empty behavior.
	 */
	@Override
	JpaJsonQueryExpression unspecifiedOnEmpty();
	/**
	 * Use the JSON empty behavior that raises an error.
	 */
	@Override
	JpaJsonQueryExpression errorOnEmpty();
	/**
	 * Use the JSON empty behavior that returns null.
	 */
	@Override
	JpaJsonQueryExpression nullOnEmpty();
	/**
	 * Use the JSON empty behavior that returns an empty array.
	 */
	@Override
	JpaJsonQueryExpression emptyArrayOnEmpty();
	/**
	 * Use the JSON empty behavior that returns an empty object.
	 */
	@Override
	JpaJsonQueryExpression emptyObjectOnEmpty();

}
