/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.util.function.UnaryOperator;

import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Allows creation of a new {@link StatelessSession} with specific options.
 *
 * @author Steve Ebersole
 *
 * @see SessionFactory#withStatelessOptions()
 */
public interface StatelessSessionBuilder extends CommonBuilder {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	StatelessSession openStatelessSession();

	@Override
	StatelessSessionBuilder connection(Connection connection);

	@Override
	StatelessSessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	StatelessSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Incubating	@Override
	StatelessSessionBuilder readOnly(boolean readOnly);

	@Incubating	@Override
	StatelessSessionBuilder initialCacheMode(CacheMode cacheMode);

	@Override
	StatelessSessionBuilder statementInspector(UnaryOperator<String> operator);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated Use {@link #tenantIdentifier(Object)} instead
	 */
	@Deprecated(since = "6.4", forRemoval = true)
	StatelessSessionBuilder tenantIdentifier(String tenantIdentifier);

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
