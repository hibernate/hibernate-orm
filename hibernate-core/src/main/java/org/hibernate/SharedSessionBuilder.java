/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.engine.creation.CommonSharedBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Specialized {@link SessionBuilder} with access to stuff from another session.
 *
 * @author Steve Ebersole
 *
 * @see Session#sessionWithOptions()
 */
public interface SharedSessionBuilder extends SessionBuilder, CommonSharedBuilder {

	@Override
	SharedSessionBuilder connection();

	@Override
	SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #connectionHandling} instead.
	 */
	@Deprecated(since = "6.0")
	SharedSessionBuilder connectionReleaseMode();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
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

	@Override @Deprecated(forRemoval = true)
	SharedSessionBuilder tenantIdentifier(String tenantIdentifier);

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
}
