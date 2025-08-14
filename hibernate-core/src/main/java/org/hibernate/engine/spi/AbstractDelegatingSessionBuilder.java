/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Base class for {@link SessionBuilder} implementations that wish to implement only parts of that contract themselves
 * while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public abstract class AbstractDelegatingSessionBuilder implements SessionBuilder {

	private final SessionBuilder delegate;

	public AbstractDelegatingSessionBuilder(SessionBuilder delegate) {
		this.delegate = delegate;
	}

	protected SessionBuilder getThis() {
		return this;
	}

	protected SessionBuilder delegate() {
		return delegate;
	}

	@Override
	public Session openSession() {
		return delegate.openSession();
	}

	@Override
	public SessionBuilder interceptor(Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return this;
	}

	@Override
	public SessionBuilder noInterceptor() {
		delegate.noInterceptor();
		return this;
	}

	@Override @Deprecated
	public SessionBuilder statementInspector(StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionBuilder statementInspector(UnaryOperator<String> operator) {
		delegate.statementInspector( operator );
		return this;
	}

	@Override
	public SessionBuilder connection(Connection connection) {
		delegate.connection( connection );
		return this;
	}

	@Override
	public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SessionBuilder autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return this;
	}

	@Override @Deprecated(forRemoval = true)
	public SessionBuilder tenantIdentifier(String tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilder tenantIdentifier(Object tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilder readOnly(boolean readOnly) {
		delegate.readOnly( readOnly );
		return this;
	}

	@Override
	public SessionBuilder eventListeners(SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return this;
	}

	@Override
	public SessionBuilder clearEventListeners() {
		delegate.clearEventListeners();
		return this;
	}

	@Override
	public SessionBuilder jdbcTimeZone(TimeZone timeZone) {
		delegate.jdbcTimeZone(timeZone);
		return this;
	}

	@Override @Deprecated
	public SessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		delegate.connectionHandlingMode( mode );
		return this;
	}

	@Override
	public SessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		delegate.connectionHandling( acquisitionMode, releaseMode );
		return this;
	}

	@Override
	public SessionBuilder autoClear(boolean autoClear) {
		delegate.autoClear( autoClear );
		return this;
	}

	@Override
	public SessionBuilder flushMode(FlushMode flushMode) {
		delegate.flushMode( flushMode );
		return this;
	}

	@Override
	public SessionBuilder identifierRollback(boolean identifierRollback) {
		delegate.identifierRollback( identifierRollback );
		return this;
	}
}
