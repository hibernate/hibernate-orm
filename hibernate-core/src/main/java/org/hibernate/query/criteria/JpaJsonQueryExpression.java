/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_query} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaJsonQueryExpression extends JpaExpression<String>, JpaJsonQueryNode {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaJsonQueryExpression passing(@Nonnull String parameterName, @Nonnull Expression<?> expression);

	/**
	 * Use the JSON query wrapper mode without a wrapper.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression withoutWrapper();
	/**
	 * Use the JSON query wrapper mode with a wrapper.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression withWrapper();
	/**
	 * Use the JSON query wrapper mode with a conditional wrapper.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression withConditionalWrapper();
	/**
	 * Use the unspecified JSON query wrapper mode.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression unspecifiedWrapper();

	/**
	 * Use the unspecified JSON error behavior.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression unspecifiedOnError();
	/**
	 * Use the JSON error behavior that raises an error.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression errorOnError();
	/**
	 * Use the JSON error behavior that returns null.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression nullOnError();
	/**
	 * Use the JSON error behavior that returns an empty array.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression emptyArrayOnError();
	/**
	 * Use the JSON error behavior that returns an empty object.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression emptyObjectOnError();

	/**
	 * Use the unspecified JSON empty behavior.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression unspecifiedOnEmpty();
	/**
	 * Use the JSON empty behavior that raises an error.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression errorOnEmpty();
	/**
	 * Use the JSON empty behavior that returns null.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression nullOnEmpty();
	/**
	 * Use the JSON empty behavior that returns an empty array.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression emptyArrayOnEmpty();
	/**
	 * Use the JSON empty behavior that returns an empty object.
	 */
	@Nonnull
	@Override
	JpaJsonQueryExpression emptyObjectOnEmpty();

}
