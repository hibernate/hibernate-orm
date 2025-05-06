/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.util.function.UnaryOperator;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Allows creation of a new {@link StatelessSession} with specific options.
 *
 * @author Steve Ebersole
 *
 * @see SessionFactory#withStatelessOptions()
 */
public interface StatelessSessionBuilder {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	StatelessSession openStatelessSession();

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	StatelessSessionBuilder connection(Connection connection);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated Use {@link #tenantIdentifier(Object)} instead
	 */
	@Deprecated(forRemoval = true)
	StatelessSessionBuilder tenantIdentifier(String tenantIdentifier);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.4
	 */
	StatelessSessionBuilder tenantIdentifier(Object tenantIdentifier);

	/**
	 * Applies the given statement inspection function to the session.
	 *
	 * @param operator An operator which accepts a SQL string, returning
	 *                 a processed SQL string to be used by Hibernate
	 *                 instead of the given original SQL. Alternatively.
	 *                 the operator may work by side effect, and simply
	 *                 return the original SQL.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @apiNote This operation exposes the SPI type
	 *          {@link StatementInspector}
	 *          and is therefore a layer-breaker.
	 */
	StatelessSessionBuilder statementInspector(UnaryOperator<String> operator);

	/**
	 * Applies the given {@link StatementInspector} to the session.
	 *
	 * @param statementInspector The {@code StatementInspector} to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated This operation exposes the SPI type{@link StatementInspector}
	 * and is therefore a layer-breaker. Use {@link #statementInspector(UnaryOperator)}
	 * instead.
	 */
	@Deprecated(since = "7.0")
	StatelessSessionBuilder statementInspector(StatementInspector statementInspector);
}
