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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

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

	protected StatementInspector statementInspector;
	protected Interceptor interceptor;
	protected boolean allowInterceptor = true;
	protected boolean allowSessionInterceptorCreation = true;
	protected Connection connection;
	protected PhysicalConnectionHandlingMode connectionHandlingMode;
	protected Object tenantIdentifier;
	protected boolean readOnly;
	protected Integer jdbcBatchSize;
	protected CacheMode cacheMode;
	protected TimeZone jdbcTimeZone;
	protected Object temporalIdentifier;

	public AbstractCommonBuilder(SessionFactoryImplementor factory) {
		sessionFactory = factory;
		final var options = factory.getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		cacheMode = options.getInitialSessionCacheMode();
		connectionHandlingMode = options.getPhysicalConnectionHandlingMode();
		jdbcTimeZone = options.getJdbcTimeZone();
		tenantIdentifier = factory.resolveTenantIdentifier();
	}

	Interceptor configuredInterceptor() {
		// If we were explicitly asked for no interceptor, always return null.
		if ( !allowInterceptor ) {
			return null;
		}

		// NOTE: DO NOT return EmptyInterceptor.INSTANCE from here as a "default for the Session".
		// 		 We "filter" that one out here. The interceptor returned here should represent the
		//		 explicitly configured Interceptor (if there is one). Return null from here instead;
		//		 Session will handle it.

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			return interceptor;
		}

		final var options = sessionFactory.getSessionFactoryOptions();

		// prefer the SessionFactory-scoped interceptor, prefer that to any Session-scoped interceptor prototype
		final var optionsInterceptor = options.getInterceptor();
		if ( optionsInterceptor != null && optionsInterceptor != EmptyInterceptor.INSTANCE ) {
			return optionsInterceptor;
		}

		if ( allowSessionInterceptorCreation ) {
			// then check the Session-scoped interceptor prototype
			final var statelessInterceptorImplementorSupplier =
					options.getStatelessInterceptorImplementorSupplier();
			if ( statelessInterceptorImplementorSupplier != null ) {
				return statelessInterceptorImplementorSupplier.get();
			}
		}

		return null;
	}

	@Nonnull
	protected abstract T getThis();

	@Override
	@Nonnull
	public T connection(@Nonnull Connection connection) {
		this.connection = connection;
		return getThis();
	}

	@Override
	@Nonnull
	public T connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode) {
		this.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret( acquisitionMode, releaseMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T interceptor(@Nullable Interceptor interceptor) {
		if ( interceptor == null ) {
			noInterceptor();
		}
		else {
			this.interceptor = interceptor;
			this.allowInterceptor = true;
		}
		return getThis();
	}

	@Override
	@Nonnull
	public T noInterceptor() {
		this.interceptor = null;
		this.allowInterceptor = false;
		return getThis();
	}

	@Override
	@Nonnull
	public T noSessionInterceptorCreation() {
		this.allowSessionInterceptorCreation = false;
		return getThis();
	}

	@Override
	@Nonnull
	public T tenantIdentifier(Object tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return getThis();
	}

	@Override
	@Nonnull
	public T jdbcBatchSize(int batchSize) {
		this.jdbcBatchSize = batchSize;
		return getThis();
	}

	@Override
	@Nonnull
	public T readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return getThis();
	}

	@Override
	@Nonnull
	public T initialCacheMode(@Nonnull CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return getThis();
	}

	@Override
	@Nonnull
	public CommonBuilder cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode) {
		return initialCacheMode( CacheMode.interpretStoreMode( cacheMode, cacheStoreMode ) );
	}

	@Override
	@Nonnull
	public CommonBuilder cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode) {
		return initialCacheMode( CacheMode.interpretRetrieveMode( cacheMode, cacheRetrieveMode ) );
	}

	@Override
	@Nonnull
	public T statementInspector(@Nullable UnaryOperator<String> operator) {
		if ( operator == null ) {
			noStatementInspector();
		}
		else {
			this.statementInspector = operator::apply;
		}
		return getThis();
	}

	@Override
	@Nonnull
	public T noStatementInspector() {
		this.statementInspector = null;
		return getThis();
	}

	@Override
	@Nonnull
	public T jdbcTimeZone(@Nullable TimeZone timeZone) {
		jdbcTimeZone = timeZone;
		return getThis();
	}

	@Override
	@Nonnull
	public T asOf(@Nullable Instant instant) {
		this.temporalIdentifier = instant;
		return getThis();
	}

	@Override
	@Nonnull
	public T atChangeset(@Nullable Object changesetId) {
		this.temporalIdentifier = changesetId;
		return getThis();
	}
}
