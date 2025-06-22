/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_exists} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonExistsExpression extends JpaExpression<Boolean>, JpaJsonExistsNode {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonExistsExpression passing(String parameterName, Expression<?> expression);

	@Override
	JpaJsonExistsExpression unspecifiedOnError();
	@Override
	JpaJsonExistsExpression errorOnError();
	@Override
	JpaJsonExistsExpression trueOnError();
	@Override
	JpaJsonExistsExpression falseOnError();
}
