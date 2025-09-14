/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.spi;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Defines the internal contract between the {@link SessionBuilder} and
 * other parts of Hibernate.
 *
 * @see SessionBuilder
 *
 * @author Gail Badner
 */
public interface SessionBuilderImplementor extends SessionBuilder {
	@Override
	SessionImplementor openSession();

	@Override
	SessionBuilderImplementor interceptor(Interceptor interceptor);

	@Override
	SessionBuilderImplementor noInterceptor();

	@Override
	SessionBuilderImplementor noSessionInterceptorCreation();

	@Override
	SessionBuilderImplementor statementInspector(UnaryOperator<String> operator);

	@Override
	SessionBuilderImplementor statementInspector(StatementInspector statementInspector);

	@Override
	SessionBuilderImplementor connection(Connection connection);

	@Override
	SessionBuilderImplementor connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	SessionBuilderImplementor connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	@Override
	SessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SessionBuilderImplementor autoClear(boolean autoClear);

	@Override
	SessionBuilderImplementor flushMode(FlushMode flushMode);

	@Override @Deprecated(forRemoval = true)
	SessionBuilderImplementor tenantIdentifier(String tenantIdentifier);

	@Override
	SessionBuilderImplementor tenantIdentifier(Object tenantIdentifier);

	@Override
	SessionBuilderImplementor readOnly(boolean readOnly);

	@Override
	SessionBuilderImplementor initialCacheMode(CacheMode cacheMode);

	@Override
	SessionBuilderImplementor eventListeners(SessionEventListener... listeners);

	@Override
	SessionBuilderImplementor clearEventListeners();

	@Override
	SessionBuilderImplementor jdbcTimeZone(TimeZone timeZone);

	@Override
	SessionBuilderImplementor autoClose(boolean autoClose);

	@Override
	SessionBuilderImplementor identifierRollback(boolean identifierRollback);

	@Override
	SessionBuilderImplementor noStatementInspector();

	@Override
	SessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize);

	@Override
	SessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled);
}
