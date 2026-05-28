/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.CommonBuilder;
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

	@Nonnull
	protected SessionBuilder getThis() {
		return this;
	}

	@Nonnull
	protected SessionBuilderImplementor delegate() {
		return delegate;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor withOption(EntityManager.CreationOption option) {
		return delegate.withOption( option );
	}

	@Override
	@Nonnull
	public SessionImplementor openSession() {
		return delegate.openSession();
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor interceptor(@Nullable Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor noInterceptor() {
		delegate.noInterceptor();
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor noSessionInterceptorCreation() {
		delegate.noSessionInterceptorCreation();
		return this;
	}

	@Override
	@Deprecated
	@Nonnull
	public SessionBuilderImplementor statementInspector(@Nonnull StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor statementInspector(@Nullable UnaryOperator<String> operator) {
		delegate.statementInspector( operator );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor noStatementInspector() {
		delegate.noStatementInspector();
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor connection(@Nonnull Connection connection) {
		delegate.connection( connection );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor tenantIdentifier(@Nullable Object tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor readOnly(boolean readOnly) {
		delegate.readOnly( readOnly );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor initialCacheMode(@Nonnull CacheMode cacheMode) {
		delegate.initialCacheMode( cacheMode );
		return this;
	}

	@Override
	@Nonnull
	public CommonBuilder jdbcBatchSize(int batchSize) {
		delegate.jdbcBatchSize( batchSize );
		return this;
	}

	@Override
	@Nonnull
	public CommonBuilder cacheStoreMode(CacheStoreMode cacheStoreMode) {
		delegate.cacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	@Nonnull
	public CommonBuilder cacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		delegate.cacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor eventListeners(@Nonnull SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor clearEventListeners() {
		delegate.clearEventListeners();
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor jdbcTimeZone(@Nullable TimeZone timeZone) {
		delegate.jdbcTimeZone(timeZone);
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public SessionBuilderImplementor connectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode mode) {
		delegate.connectionHandlingMode( mode );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode) {
		delegate.connectionHandling( acquisitionMode, releaseMode );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor autoClear(boolean autoClear) {
		delegate.autoClear( autoClear );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor flushMode(@Nonnull FlushMode flushMode) {
		delegate.flushMode( flushMode );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		delegate.identifierRollback( identifierRollback );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize) {
		delegate.defaultBatchFetchSize( defaultBatchFetchSize );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled) {
		delegate.subselectFetchEnabled( subselectFetchEnabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor asOf(@Nullable Instant instant) {
		delegate.asOf( instant );
		return this;
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor atChangeset(@Nullable Object changesetId) {
		delegate.atChangeset( changesetId );
		return this;
	}
}
