/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.engine.creation.CommonSharedBuilder;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Allows creation of a child {@link StatelessSession} which shares some options
 * with another pre-existing parent session.
 * <p>
 * When {@linkplain Transaction resource-local} transaction management is used:
 * <ul>
 * <li>by default, each session executes with its own dedicated JDBC connection
 *     and therefore has its own isolated transaction, but
 * <li>calling the {@link #connection()} method specifies that the connection,
 *     and therefore also the JDBC transaction, should be shared from parent
 *     to child.
 * </ul>
 * <p>
 * <pre>
 * try (var statelessSession
 *          = session.statelessWithOptions()
 *                  .connection() // share the JDBC connection
 *                  .cacheMode(CacheMode.IGNORE)
 *                  .openStatelessSession()) {
 *     ...
 * }
 * </pre>
 * <p>
 * On the other hand, when JTA transaction management is used, all sessions
 * execute within the same transaction. Typically, connection sharing is
 * handled automatically by the JTA-enabled {@link javax.sql.DataSource}.
 *
 * @see Session#statelessWithOptions()
 * @see StatelessSession#statelessWithOptions()
 * @see SharedSessionBuilder
 *
 * @since 7.2
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SharedStatelessSessionBuilder extends StatelessSessionBuilder, CommonSharedBuilder {
	/**
	 * Open the stateless session.
	 */
	StatelessSession open();

	@Override
	SharedStatelessSessionBuilder connection();

	@Override
	SharedStatelessSessionBuilder interceptor();

	@Override
	SharedStatelessSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedStatelessSessionBuilder noInterceptor();

	@Override
	SharedStatelessSessionBuilder noSessionInterceptorCreation();

	SharedStatelessSessionBuilder statementInspector(UnaryOperator<String> operator);

	@Override
	SharedStatelessSessionBuilder statementInspector();

	@Override
	SharedStatelessSessionBuilder noStatementInspector();

	@Override
	SharedStatelessSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	SharedStatelessSessionBuilder readOnly(boolean readOnly);

	@Override
	SharedStatelessSessionBuilder initialCacheMode(CacheMode cacheMode);

	@Override
	SharedStatelessSessionBuilder connection(Connection connection);

	@Override
	SharedStatelessSessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	SharedStatelessSessionBuilder jdbcTimeZone(TimeZone timeZone);
}
