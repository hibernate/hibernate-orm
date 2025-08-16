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
	@Deprecated(since = "6.4", forRemoval = true)
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
	 * Specify a read-only mode for the stateless session. If a session
	 * is created in read-only mode, then {@link Connection#setReadOnly}
	 * is called when a JDBC connection is obtained.
	 * <p>
	 * Furthermore, if read/write replication is in use, then:
	 * <ul>
	 * <li>a read-only session will connect to a read-only replica, but
	 * <li>a non-read-only session will connect to a writable replica.
	 * </ul>
	 * <p>
	 * When read/write replication is in use, it's strongly recommended
	 * that the session be created with the {@linkplain #initialCacheMode
	 * initial cache mode} set to {@link CacheMode#GET}, to avoid writing
	 * stale data read from a read-only replica to the second-level cache.
	 * Hibernate cannot possibly guarantee that data read from a read-only
	 * replica is up to date. It's also possible for a read-only session to
	 * <p>
	 * When read/write replication is in use, it's possible that an item
	 * read from the second-level cache might refer to data which does not
	 * yet exist in the read-only replica. In this situation, an exception
	 * occurs when the association is fetched. To completely avoid this
	 * possibility, the {@linkplain #initialCacheMode initial cache mode}
	 * must be set to {@link CacheMode#IGNORE}. However, it's also usually
	 * possible to structure data access code in a way which eliminates
	 * this possibility.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#getReadOnlyConnection(Object)
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#releaseReadOnlyConnection(Object, Connection)
	 */
	@Incubating
	StatelessSessionBuilder readOnly(boolean readOnly);

	/**
	 * Specify the initial {@link CacheMode} for the session.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see SharedSessionContract#getCacheMode()
	 */
	StatelessSessionBuilder initialCacheMode(CacheMode cacheMode);

	/**
	 * Applies the given statement inspection function to the session.
	 *
	 * @param operator An operator which accepts a SQL string, returning
	 *                 a processed SQL string to be used by Hibernate
	 *                 instead of the given original SQL. Alternatively,
	 *                 the operator may work by side effect and simply
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
