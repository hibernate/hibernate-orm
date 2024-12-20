/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;

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
	 * Applies the given {@link StatementInspector} to the stateless session.
	 *
	 * @param statementInspector The StatementInspector to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	StatelessSessionBuilder statementInspector(StatementInspector statementInspector);
}
