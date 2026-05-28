/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

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
	Session open();

	@Override
	SharedSessionBuilder connection();

	@Override
	SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection handling mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder connectionHandlingMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoJoinTransactions();

	/**
	 * Signifies that the FlushMode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder flushMode();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoClose();

	@Override
	SharedSessionBuilder asOf(Instant instant);

	@Override
	SharedSessionBuilder atChangeset(Object changesetId);

	@Override @Deprecated
	SharedSessionBuilder statementInspector(StatementInspector statementInspector);

	@Override
	SharedSessionBuilder statementInspector(UnaryOperator<String> operator);

	@Override
	SharedSessionBuilder statementInspector();

	@Override
	SharedSessionBuilder noStatementInspector();

	@Override @Deprecated
	SharedSessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	@Override
	SharedSessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	SharedSessionBuilder autoClear(boolean autoClear);

	@Override
	SharedSessionBuilder flushMode(FlushMode flushMode);

	@Override
	SharedSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	SharedSessionBuilder readOnly(boolean readOnly);

	@Override
	SharedSessionBuilder initialCacheMode(CacheMode cacheMode);

	@Override
	SharedSessionBuilder eventListeners(SessionEventListener... listeners);

	@Override
	SharedSessionBuilder clearEventListeners();

	@Override
	SharedSessionBuilder jdbcTimeZone(TimeZone timeZone);

	@Override
	SharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedSessionBuilder noInterceptor();

	@Override
	SharedSessionBuilder noSessionInterceptorCreation();

	@Override
	SharedSessionBuilder connection(Connection connection);

	@Override
	SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SharedSessionBuilder autoClose(boolean autoClose);

	@Override
	SharedSessionBuilder identifierRollback(boolean identifierRollback);

	@Override
	SharedSessionBuilder defaultBatchFetchSize(int defaultBatchFetchSize);

	@Override
	SharedSessionBuilder subselectFetchEnabled(boolean subselectFetchEnabled);
}
