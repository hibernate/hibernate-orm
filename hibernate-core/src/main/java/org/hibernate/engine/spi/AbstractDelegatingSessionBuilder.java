/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Base class for {@link SessionBuilder} implementations that wish to implement only parts of that contract themselves
 * while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public abstract class AbstractDelegatingSessionBuilder implements SessionBuilderImplementor {

	private final SessionBuilderImplementor delegate;

	public AbstractDelegatingSessionBuilder(SessionBuilder delegate) {
		this.delegate = (SessionBuilderImplementor) delegate;
	}

	protected SessionBuilder getThis() {
		return this;
	}

	protected SessionBuilderImplementor delegate() {
		return delegate;
	}

	@Override
	public SessionImplementor openSession() {
		return delegate.openSession();
	}

	@Override
	public SessionBuilderImplementor interceptor(Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return this;
	}

	@Override
	public SessionBuilderImplementor noInterceptor() {
		delegate.noInterceptor();
		return this;
	}

	@Override @Deprecated
	public SessionBuilderImplementor statementInspector(StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionBuilderImplementor statementInspector(UnaryOperator<String> operator) {
		delegate.statementInspector( operator );
		return this;
	}

	@Override
	public SessionBuilderImplementor noStatementInspector() {
		delegate.noStatementInspector();
		return this;
	}

	@Override
	public SessionBuilderImplementor connection(Connection connection) {
		delegate.connection( connection );
		return this;
	}

	@Override
	public SessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SessionBuilderImplementor autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return this;
	}

	@Override @Deprecated(forRemoval = true)
	public SessionBuilderImplementor tenantIdentifier(String tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilderImplementor tenantIdentifier(Object tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilderImplementor readOnly(boolean readOnly) {
		delegate.readOnly( readOnly );
		return this;
	}

	@Override
	public SessionBuilderImplementor initialCacheMode(CacheMode cacheMode) {
		delegate.initialCacheMode( cacheMode );
		return this;
	}

	@Override
	public SessionBuilderImplementor eventListeners(SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return this;
	}

	@Override
	public SessionBuilderImplementor clearEventListeners() {
		delegate.clearEventListeners();
		return this;
	}

	@Override
	public SessionBuilderImplementor jdbcTimeZone(TimeZone timeZone) {
		delegate.jdbcTimeZone(timeZone);
		return this;
	}

	@Override @Deprecated
	public SessionBuilderImplementor connectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		delegate.connectionHandlingMode( mode );
		return this;
	}

	@Override
	public SessionBuilderImplementor connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		delegate.connectionHandling( acquisitionMode, releaseMode );
		return this;
	}

	@Override
	public SessionBuilderImplementor autoClear(boolean autoClear) {
		delegate.autoClear( autoClear );
		return this;
	}

	@Override
	public SessionBuilderImplementor flushMode(FlushMode flushMode) {
		delegate.flushMode( flushMode );
		return this;
	}

	@Override
	public SessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		delegate.identifierRollback( identifierRollback );
		return this;
	}
}
