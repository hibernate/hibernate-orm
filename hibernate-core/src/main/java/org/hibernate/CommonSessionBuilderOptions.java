/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.function.UnaryOperator;

/**
 * Common options builders of {@linkplain Session} and {@linkplain StatelessSession}.
 *
 * @author Steve Ebersole
 */
public interface CommonSessionBuilderOptions {
	/**
	 * Adds a specific interceptor to the session options.
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSessionBuilderOptions interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p>
	 * By default, if no {@code Interceptor} is explicitly specified, the
	 * {@code Interceptor} associated with the {@link SessionFactory} is
	 * inherited by the new session.
	 * <p>
	 * Calling {@link #interceptor(Interceptor)} with null has the same effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSessionBuilderOptions noInterceptor();

	/**
	 * Applies the given statement inspection function to the session.
	 *
	 * @param operator An operator which accepts a SQL string, returning
	 * a processed SQL string to be used by Hibernate instead of the given
	 * original SQL. The operator may simply return the original SQL.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSessionBuilderOptions statementInspector(UnaryOperator<String> operator);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSessionBuilderOptions tenantIdentifier(Object tenantIdentifier);
}
