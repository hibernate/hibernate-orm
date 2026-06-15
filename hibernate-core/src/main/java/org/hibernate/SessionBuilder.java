/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.cfg.StateManagementSettings;
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
/// ```
///
/// @see SessionFactory#withOptions()
/// @see SharedSessionBuilder
///
/// @author Steve Ebersole
public interface SessionBuilder extends CommonBuilder {
	/// Open the session using the specified options.
	/// @see #open
	@Nonnull
	Session openSession();

	@Override
	@Nonnull
	default Session open() {
		return openSession();
	}

	@Override
	@Nonnull
	SessionBuilder interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	SessionBuilder noInterceptor();

	@Override
	@Nonnull
	SessionBuilder noSessionInterceptorCreation();

	@Override
	@Nonnull
	SessionBuilder noStatementInspector();

	@Override
	@Nonnull
	SessionBuilder statementInspector(@Nullable UnaryOperator<String> operator);

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
	@Nonnull
	SessionBuilder statementInspector(@Nonnull StatementInspector statementInspector);

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Override
	@Nonnull
	SessionBuilder connection(@Nonnull Connection connection);

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
	@Override
	@Nonnull
	SessionBuilder connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

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
	@Nonnull
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
	@Nonnull
	SessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should the session be automatically cleared on a failed transaction?
	 *
	 * @param autoClear Whether the Session should be automatically cleared
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
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
	@Nonnull
	SessionBuilder flushMode(@Nonnull FlushMode flushMode);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.4
	 */
	@Override
	@Nonnull
	SessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	@Nonnull
	SessionBuilder readOnly(boolean readOnly);

	@Override
	@Nonnull
	SessionBuilder jdbcBatchSize(int batchSize);

	@Override
	@Nonnull
	SessionBuilder initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	SessionBuilder cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	SessionBuilder cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode);

	/**
	 * Add one or more {@link SessionEventListener} instances to the list of
	 * listeners for the new session to be built.
	 *
	 * @param listeners The listeners to incorporate into the built Session
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SessionBuilder eventListeners(@Nonnull SessionEventListener... listeners);

	/**
	 * Remove all listeners intended for the built session currently held here,
	 * including any auto-apply ones; in other words, start with a clean slate.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SessionBuilder clearEventListeners();

	/**
	 * Specify the {@linkplain org.hibernate.cfg.JdbcSettings#JDBC_TIME_ZONE
	 * JDBC time zone} for the session.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Override
	@Nonnull
	SessionBuilder jdbcTimeZone(@Nullable TimeZone timeZone);

	/**
	 * Should the session be automatically closed after transaction completion?
	 *
	 * @param autoClose Should the session be automatically closed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.PersistenceContextType
	 */
	@Nonnull
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
	@Nonnull
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
	@Nonnull
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
	@Nonnull
	SessionBuilder subselectFetchEnabled(boolean subselectFetchEnabled);

	/**
	 * Specify the instant for reading
	 * {@linkplain org.hibernate.annotations.Temporal temporal} entity data.
	 * Instances of temporal entities retrieved in the session represent the
	 * revisions effective at the given instant.
	 */
	@Nonnull
	SessionBuilder asOf(@Nullable Instant instant);

	/**
	 * Specify the
	 * {@linkplain StateManagementSettings#CHANGESET_ID_SUPPLIER
	 * changeset id} for reading {@linkplain org.hibernate.annotations.Temporal
	 * temporal} or {@linkplain org.hibernate.annotations.Audited audited}
	 * entity data. Instances of temporal or audited entities retrieved in
	 * the session represent the state effective at the given changeset.
	 */
	@Nonnull
	SessionBuilder atChangeset(@Nullable Object changesetId);
}
