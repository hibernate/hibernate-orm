/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.engine.creation.CommonSharedBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Allows creation of a child {@link Session} which shares some options with
 * another pre-existing parent session. Each session has its own isolated
 * persistence context, and entity instances must not be shared between
 * parent and child sessions.
 * <p>
 * A child session does not, by default, share the parent's JDBC connection,
 * transaction coordinator, persistence context, action queue, interceptor, or
 * SQL statement inspector. Instead, the child session starts from the normal
 * {@link SessionFactory} defaults, with the following values inherited from the
 * parent session as the baseline:
 * <ul>
 * <li>the tenant identifier,
 * <li>the JDBC time zone,
 * <li>the temporal or changeset identifier used for loading temporal data, and
 * <li>whether identifier rollback is enabled.
 * </ul>
 * Other stateful session options are inherited only when explicitly requested
 * by the corresponding no-argument method, for example {@link #connection()},
 * {@link #interceptor()}, {@link #statementInspector()}, {@link #flushMode()},
 * {@link #autoJoinTransactions()}, {@link #autoClose()}, or
 * {@link #connectionHandlingMode()}.
 * <p>
 * When {@linkplain Transaction resource-local} transaction management is used:
 * <ul>
 * <li>by default, each session executes with its own dedicated JDBC connection
 *     and therefore has its own isolated transaction, but
 * <li>calling the {@link #connection()} method specifies that the connection,
 *     and therefore also the JDBC transaction, should be shared from parent
 *     to child.
 * </ul>
 * A child session with a shared transaction context also receives parent flush
 * and close notifications, so the child's work is flushed with the parent, and
 * the child is automatically closed when the parent is closed.
 * <pre>
 * try (var childSession
 *          = session.sessionWithOptions()
 *                  .connection() // share the JDBC connection
 *                  .cacheMode(CacheMode.IGNORE)
 *                  .openSession()) {
 *     ...
 * }
 * </pre>
 * On the other hand, when JTA transaction management is used, all sessions
 * execute within the same transaction. Typically, connection sharing is
 * handled automatically by the JTA-enabled {@link javax.sql.DataSource}.
 *
 * @author Steve Ebersole
 *
 * @see Session#sessionWithOptions()
 * @see StatelessSession#sessionWithOptions()
 * @see SessionBuilder
 */
public interface SharedSessionBuilder extends SessionBuilder, CommonSharedBuilder {
	/**
	 * Open the session.
	 */
	@Override
	@Nonnull
	Session open();

	@Override
	@Nonnull
	SharedSessionBuilder connection();

	@Override
	@Nonnull
	SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection handling mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SharedSessionBuilder connectionHandlingMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SharedSessionBuilder autoJoinTransactions();

	/**
	 * Signifies that the FlushMode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SharedSessionBuilder flushMode();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	SharedSessionBuilder autoClose();

	@Override
	@Nonnull
	SharedSessionBuilder asOf(@Nullable Instant instant);

	@Override
	@Nonnull
	SharedSessionBuilder atChangeset(@Nullable Object changesetId);

	@Override
	@Deprecated
	@Nonnull
	SharedSessionBuilder statementInspector(@Nonnull StatementInspector statementInspector);

	@Override
	@Nonnull
	SharedSessionBuilder statementInspector(@Nullable UnaryOperator<String> operator);

	@Override
	@Nonnull
	SharedSessionBuilder statementInspector();

	@Override
	@Nonnull
	SharedSessionBuilder noStatementInspector();

	@Override
	@Deprecated
	@Nonnull
	SharedSessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	@Override
	@Nonnull
	SharedSessionBuilder connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

	@Override
	@Nonnull
	SharedSessionBuilder autoClear(boolean autoClear);

	@Override
	@Nonnull
	SharedSessionBuilder flushMode(@Nonnull FlushMode flushMode);

	@Override
	@Nonnull
	SharedSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	@Nonnull
	SharedSessionBuilder readOnly(boolean readOnly);

	@Override
	@Nonnull
	SharedSessionBuilder jdbcBatchSize(int batchSize);

	@Override
	@Nonnull
	SharedSessionBuilder initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	SharedSessionBuilder cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	SharedSessionBuilder cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Nonnull
	SharedSessionBuilder eventListeners(@Nonnull SessionEventListener... listeners);

	@Override
	@Nonnull
	SharedSessionBuilder clearEventListeners();

	@Override
	@Nonnull
	SharedSessionBuilder jdbcTimeZone(@Nullable TimeZone timeZone);

	@Override
	@Nonnull
	SharedSessionBuilder interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	SharedSessionBuilder noInterceptor();

	@Override
	@Nonnull
	SharedSessionBuilder noSessionInterceptorCreation();

	@Override
	@Nonnull
	SharedSessionBuilder connection(@Nonnull Connection connection);

	@Override
	@Nonnull
	SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	@Nonnull
	SharedSessionBuilder autoClose(boolean autoClose);

	@Override
	@Nonnull
	SharedSessionBuilder identifierRollback(boolean identifierRollback);

	@Override
	@Nonnull
	SharedSessionBuilder defaultBatchFetchSize(int defaultBatchFetchSize);

	@Override
	@Nonnull
	SharedSessionBuilder subselectFetchEnabled(boolean subselectFetchEnabled);
}
