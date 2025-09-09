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
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * @author Steve Ebersole
 *
 * @since 7.2
 */
public interface SharedSessionBuilderImplementor extends SharedSessionBuilder, SessionBuilderImplementor {
	@Override
	SharedSessionBuilderImplementor interceptor(Interceptor interceptor);

	@Override
	SharedSessionBuilderImplementor noInterceptor();

	@Override
	SharedSessionBuilderImplementor statementInspector(UnaryOperator<String> operator);

	@Override
	SharedSessionBuilderImplementor statementInspector(StatementInspector statementInspector);

	@Override
	SharedSessionBuilderImplementor tenantIdentifier(Object tenantIdentifier);

	@Override
	SharedSessionBuilderImplementor readOnly(boolean readOnly);

	@Override
	SharedSessionBuilderImplementor initialCacheMode(CacheMode cacheMode);


	@Override
	SharedSessionBuilderImplementor connection();

	@Override
	SharedSessionBuilderImplementor interceptor();

	@Override
	SharedSessionBuilderImplementor connectionReleaseMode();

	@Override
	SharedSessionBuilderImplementor connectionHandlingMode();

	@Override
	SharedSessionBuilderImplementor autoJoinTransactions();

	@Override
	SharedSessionBuilderImplementor flushMode();

	@Override
	SharedSessionBuilderImplementor autoClose();

	@Override
	SharedSessionBuilderImplementor connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	@Override
	SharedSessionBuilderImplementor connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	SharedSessionBuilderImplementor autoClear(boolean autoClear);

	@Override
	SharedSessionBuilderImplementor flushMode(FlushMode flushMode);

	@Override
	SharedSessionBuilderImplementor tenantIdentifier(String tenantIdentifier);

	@Override
	SharedSessionBuilderImplementor eventListeners(SessionEventListener... listeners);

	@Override
	SharedSessionBuilderImplementor clearEventListeners();

	@Override
	SharedSessionBuilderImplementor jdbcTimeZone(TimeZone timeZone);

	@Override
	SharedSessionBuilderImplementor connection(Connection connection);

	@Override
	SharedSessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SharedSessionBuilderImplementor autoClose(boolean autoClose);

	@Override
	SharedSessionBuilderImplementor identifierRollback(boolean identifierRollback);

	@Override
	SharedSessionBuilderImplementor statementInspector();

	@Override
	SharedSessionBuilderImplementor noStatementInspector();
}
