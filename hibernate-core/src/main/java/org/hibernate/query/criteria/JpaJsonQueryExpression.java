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

	@Override
	JpaJsonQueryExpression withoutWrapper();
	@Override
	JpaJsonQueryExpression withWrapper();
	@Override
	JpaJsonQueryExpression withConditionalWrapper();
	@Override
	JpaJsonQueryExpression unspecifiedWrapper();

	@Override
	JpaJsonQueryExpression unspecifiedOnError();
	@Override
	JpaJsonQueryExpression errorOnError();
	@Override
	JpaJsonQueryExpression nullOnError();
	@Override
	JpaJsonQueryExpression emptyArrayOnError();
	@Override
	JpaJsonQueryExpression emptyObjectOnError();

	@Override
	JpaJsonQueryExpression unspecifiedOnEmpty();
	@Override
	JpaJsonQueryExpression errorOnEmpty();
	@Override
	JpaJsonQueryExpression nullOnEmpty();
	@Override
	JpaJsonQueryExpression emptyArrayOnEmpty();
	@Override
	JpaJsonQueryExpression emptyObjectOnEmpty();

}
