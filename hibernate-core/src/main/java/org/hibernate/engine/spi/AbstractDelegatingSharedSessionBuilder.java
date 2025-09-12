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
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Base class for {@link SharedSessionBuilder} implementations that wish to implement only parts of that contract
 * themselves while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public abstract class AbstractDelegatingSharedSessionBuilder implements SharedSessionBuilder {
	private final SharedSessionBuilder delegate;

	public AbstractDelegatingSharedSessionBuilder(SharedSessionBuilder delegate) {
		this.delegate = delegate;
	}

	protected SharedSessionBuilder getThis() {
		return this;
	}

	public SharedSessionBuilder delegate() {
		return delegate;
	}

	@Override
	public Session openSession() {
		return delegate.openSession();
	}

	@Override
	public SharedSessionBuilder interceptor() {
		delegate.interceptor();
		return this;
	}

	@Override
	public SharedSessionBuilder connection() {
		delegate.connection();
		return this;
	}

	@Override @Deprecated(since = "6.0")
	public SharedSessionBuilder connectionReleaseMode() {
		delegate.connectionReleaseMode();
		return this;
	}

	@Override
	public SharedSessionBuilder connectionHandlingMode() {
		delegate.connectionHandlingMode();
		return this;
	}

	@Override
	public SharedSessionBuilder autoJoinTransactions() {
		delegate.autoJoinTransactions();
		return this;
	}

	@Override
	public SharedSessionBuilder autoClose() {
		delegate.autoClose();
		return this;
	}

	@Override
	public SharedSessionBuilder interceptor(Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return this;
	}

	@Override
	public SharedSessionBuilder noInterceptor() {
		delegate.noInterceptor();
		return this;
	}

	@Override
	public SharedSessionBuilder noSessionInterceptorCreation() {
		delegate.noSessionInterceptorCreation();
		return this;
	}

	@Override @Deprecated
	public SharedSessionBuilder statementInspector(StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return this;
	}

	@Override
	public SharedSessionBuilder statementInspector(UnaryOperator<String> operator) {
		delegate.statementInspector( operator );
		return this;
	}

	@Override
	public SharedSessionBuilder statementInspector() {
		delegate.statementInspector();
		return this;
	}

	@Override
	public SharedSessionBuilder noStatementInspector() {
		delegate.noStatementInspector();
		return this;
	}

	@Override
	public SharedSessionBuilder connection(Connection connection) {
		delegate.connection( connection );
		return this;
	}

	@Override
	public SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SharedSessionBuilder autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return this;
	}

	@Override @Deprecated(forRemoval = true)
	public SharedSessionBuilder tenantIdentifier(String tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SharedSessionBuilder tenantIdentifier(Object tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SharedSessionBuilder readOnly(boolean readOnly) {
		delegate.readOnly( readOnly );
		return this;
	}

	@Override
	public SharedSessionBuilder initialCacheMode(CacheMode cacheMode) {
		delegate.initialCacheMode( cacheMode );
		return this;
	}

	@Override
	public SharedSessionBuilder eventListeners(SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return this;
	}

	@Override
	public SharedSessionBuilder clearEventListeners() {
		delegate.clearEventListeners();
		return this;
	}

	@Override @Deprecated
	public SharedSessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		delegate.connectionHandlingMode( mode );
		return this;
	}

	@Override
	public SharedSessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		delegate.connectionHandling( acquisitionMode, releaseMode );
		return this;
	}

	@Override
	public SharedSessionBuilder autoClear(boolean autoClear) {
		delegate.autoClear( autoClear );
		return this;
	}

	@Override
	public SharedSessionBuilder flushMode(FlushMode flushMode) {
		delegate.flushMode( flushMode );
		return this;
	}

	@Override
	public SharedSessionBuilder flushMode() {
		delegate.flushMode();
		return this;
	}

	@Override
	public SharedSessionBuilder jdbcTimeZone(TimeZone timeZone) {
		delegate.jdbcTimeZone( timeZone );
		return this;
	}

	@Override
	public SharedSessionBuilder identifierRollback(boolean identifierRollback) {
		delegate.identifierRollback( identifierRollback );
		return this;
	}
}
