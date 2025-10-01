/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/// Allows creation of a new [Session] with specific options
/// overriding the defaults from the [SessionFactory].
///
/// ```java
/// try (var session = sessionFactory.withOptions()
/// 		.tenantIdentifier(tenantId)
/// 		.initialCacheMode(CacheMode.PUT)
/// 		.flushMode(FlushMode.COMMIT)
/// 		.interceptor(new Interceptor() {
/// 			@Override
/// 			public void preFlush(Iterator<Object> entities) {
/// 				...
/// 			}
/// 		})
/// 		.openSession()) {
/// 			...
/// 		}
/// }
/// @author Steve Ebersole
///
/// @see SessionFactory#withOptions()
/// @see SharedSessionBuilder
public interface SessionBuilder extends CommonBuilder {
	/// Open the session using the specified options.
	/// @see #open
	Session openSession();

	@Override
	default Session open() {
		return openSession();
	}

	@Override
	SessionBuilder interceptor(Interceptor interceptor);

	@Override
	SessionBuilder noInterceptor();

	@Override
	SessionBuilder noSessionInterceptorCreation();

	@Override
	SessionBuilder noStatementInspector();

	@Override
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
	@Override
	SessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	SessionBuilder readOnly(boolean readOnly);

	@Override
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
	@Override
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

	/**
	 * Specify the default batch fetch size for the session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.FetchSettings#DEFAULT_BATCH_FETCH_SIZE
	 * @see Session#setFetchBatchSize(int)
	 *
	 * @since 7.2
	 */
	SessionBuilder defaultBatchFetchSize(int defaultBatchFetchSize);

	/**
	 * Specify whether subselect fetching is enabled for the session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.FetchSettings#USE_SUBSELECT_FETCH
	 * @see Session#setSubselectFetchingEnabled(boolean)
	 *
	 * @since 7.2
	 */
	SessionBuilder subselectFetchEnabled(boolean subselectFetchEnabled);
}
