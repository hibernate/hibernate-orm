/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.engine.creation.internal.options.CommonOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Base support for session builders.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCommonBuilder<T extends CommonBuilder> implements CommonBuilder {
	protected final SessionFactoryImplementor sessionFactory;
	protected final CommonOptions options;

	public AbstractCommonBuilder(SessionFactoryImplementor factory) {
		this( factory, new CommonOptions( factory ) );
	}

	protected AbstractCommonBuilder(SessionFactoryImplementor factory, CommonOptions options) {
		sessionFactory = factory;
		this.options = options;
	}

	protected CommonOptions options() {
		return options;
	}

	protected abstract T getThis();

	@Override
	@Nonnull
	public T connection(@Nonnull Connection connection) {
		options.connection( connection );
		return getThis();
	}

	@Override
	@Nonnull
	public T connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode) {
		options.connectionHandling( acquisitionMode, releaseMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T interceptor(@Nullable Interceptor interceptor) {
		options.interceptor( interceptor );
		return getThis();
	}

	@Override
	@Nonnull
	public T noInterceptor() {
		options.noInterceptor();
		return getThis();
	}

	@Override
	@Nonnull
	public T noSessionInterceptorCreation() {
		options.noSessionInterceptorCreation();
		return getThis();
	}

	@Override
	@Nonnull
	public T tenantIdentifier(Object tenantIdentifier) {
		options.tenantIdentifier( tenantIdentifier );
		return getThis();
	}

	@Override
	@Nonnull
	public T jdbcBatchSize(int batchSize) {
		options.jdbcBatchSize( batchSize );
		return getThis();
	}

	@Override
	@Nonnull
	public T readOnly(boolean readOnly) {
		options.readOnly( readOnly );
		return getThis();
	}

	@Override
	@Nonnull
	public T initialCacheMode(@Nonnull CacheMode cacheMode) {
		options.initialCacheMode( cacheMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode) {
		options.cacheStoreMode( cacheStoreMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode) {
		options.cacheRetrieveMode( cacheRetrieveMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T statementInspector(@Nullable UnaryOperator<String> operator) {
		options.statementInspector( operator );
		return getThis();
	}

	@Override
	@Nonnull
	public T noStatementInspector() {
		options.noStatementInspector();
		return getThis();
	}

	@Override
	@Nonnull
	public T jdbcTimeZone(@Nullable TimeZone timeZone) {
		options.jdbcTimeZone( timeZone );
		return getThis();
	}

	@Override
	@Nonnull
	public T asOf(@Nullable Instant instant) {
		options.asOf( instant );
		return getThis();
	}

	@Override
	@Nonnull
	public T atChangeset(@Nullable Object changesetId) {
		options.atChangeset( changesetId );
		return getThis();
	}
}
