/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Allows creation of a new {@link Session} with specific options.
 *
 * @author Steve Ebersole
 *
 * @see SessionFactory#withOptions()
 */
public interface SessionBuilder {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	Session openSession();

	/**
	 * Adds a specific interceptor to the session options.
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p>
	 * By default, if no {@code Interceptor} is explicitly specified, the
	 * {@code Interceptor} associated with the {@link SessionFactory} is
	 * inherited by the new {@link Session}.
	 * <p>
	 * Calling {@link #interceptor(Interceptor)} with null has the same effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder noInterceptor();

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
	 * @since 7.0
	 */
	SessionBuilder statementInspector(UnaryOperator<String> operator);

	/**
	 * Applies the given {@link StatementInspector} to the session.
	 *
	 * @param statementInspector The {@code StatementInspector} to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated This operation exposes the SPI type {@link StatementInspector}
	 * and is therefore a layer-breaker. Use {@link #statementInspector(UnaryOperator)}
	 * instead.
	 */
	@Deprecated(since = "7.0")
	SessionBuilder statementInspector(StatementInspector statementInspector);

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder connection(Connection connection);

	/**
	 * Specifies the connection handling modes for the session.
	 * <p>
	 * Note that if {@link ConnectionAcquisitionMode#IMMEDIATELY} is specified,
	 * then the release mode must be {@link ConnectionReleaseMode#ON_CLOSE}.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 7.0
	 */
	SessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	/**
	 * Specifies the {@linkplain PhysicalConnectionHandlingMode connection handling mode}.
	 *
	 * @param mode The connection handling mode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated This operation exposes the SPI type
	 * {@link PhysicalConnectionHandlingMode} and is therefore a layer-breaker.
	 * Use {@link #connectionHandling(ConnectionAcquisitionMode, ConnectionReleaseMode)}
	 * instead.
	 */
	@Deprecated(since = "7.0")
	SessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	/**
	 * Should the session built automatically join in any ongoing JTA transactions.
	 *
	 * @param autoJoinTransactions Should JTA transactions be automatically joined
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.SynchronizationType#SYNCHRONIZED
	 */
	SessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should the session be automatically cleared on a failed transaction?
	 *
	 * @param autoClear Whether the Session should be automatically cleared
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder autoClear(boolean autoClear);

	/**
	 * Specify the initial {@link FlushMode} to use for the opened Session
	 *
	 * @param flushMode The initial {@code FlushMode} to use for the opened Session
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.PersistenceContextType
	 */
	SessionBuilder flushMode(FlushMode flushMode);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated Use {@link #tenantIdentifier(Object)} instead
	 */
	@Deprecated(since = "6.4", forRemoval = true)
	SessionBuilder tenantIdentifier(String tenantIdentifier);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.4
	 */
	SessionBuilder tenantIdentifier(Object tenantIdentifier);

	/**
	 * Specify a {@linkplain Session#isDefaultReadOnly read-only mode}
	 * for the session. If a session is created in read-only mode, then
	 * {@link Connection#setReadOnly} is called when a JDBC connection
	 * is obtained.
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
	 * replica is up to date.
	 * <p>
	 * When read/write replication is in use, it's possible that an item
	 * read from the second-level cache might refer to data which does not
	 * yet exist in the read-only replica. In this situation, an exception
	 * occurs when the association is fetched. To completely avoid this
	 * possibility, the {@linkplain #initialCacheMode initial cache mode}
	 * must be set to {@link CacheMode#IGNORE}. However, it's also usually
	 * possible to structure data access code in a way which eliminates
	 * this possibility.
	 * <p>
	 * If a session is created in read-only mode, then it cannot be
	 * changed to read-write mode, and any call to
	 * {@link Session#setDefaultReadOnly(boolean)} with fail. On the
	 * other hand, if a session is created in read-write mode, then it
	 * may later be switched to read-only mode, but all database access
	 * is directed to the writable replica.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#getReadOnlyConnection(Object)
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#releaseReadOnlyConnection(Object, Connection)
	 */
	@Incubating
	SessionBuilder readOnly(boolean readOnly);

	/**
	 * Specify the initial {@link CacheMode} for the session.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see SharedSessionContract#getCacheMode()
	 */
	SessionBuilder initialCacheMode(CacheMode cacheMode);

	/**
	 * Add one or more {@link SessionEventListener} instances to the list of
	 * listeners for the new session to be built.
	 *
	 * @param listeners The listeners to incorporate into the built Session
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder eventListeners(SessionEventListener... listeners);

	/**
	 * Remove all listeners intended for the built session currently held here,
	 * including any auto-apply ones; in other words, start with a clean slate.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder clearEventListeners();

	/**
	 * Specify the {@linkplain org.hibernate.cfg.JdbcSettings#JDBC_TIME_ZONE
	 * JDBC time zone} for the session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionBuilder jdbcTimeZone(TimeZone timeZone);

	/**
	 * Should the session be automatically closed after transaction completion?
	 *
	 * @param autoClose Should the session be automatically closed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.PersistenceContextType
	 */
	SessionBuilder autoClose(boolean autoClose);

	/**
	 * Enable identifier rollback after entity removal for the session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_IDENTIFIER_ROLLBACK
	 *
	 * @since 7.0
	 */
	SessionBuilder identifierRollback(boolean identifierRollback);
}
