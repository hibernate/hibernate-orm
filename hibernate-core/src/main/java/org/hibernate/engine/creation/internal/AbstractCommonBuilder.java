/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.function.UnaryOperator;

/**
 * Base support for session builders.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCommonBuilder<T extends CommonBuilder> implements CommonBuilder {
	protected final SessionFactoryImpl sessionFactory;

	protected StatementInspector statementInspector;
	protected Interceptor interceptor;
	protected boolean explicitNoInterceptor;
	protected Connection connection;
	protected PhysicalConnectionHandlingMode connectionHandlingMode;
	protected Object tenantIdentifier;
	protected boolean readOnly;
	protected CacheMode cacheMode;

	public AbstractCommonBuilder(SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;

		final var options = sessionFactory.getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		cacheMode = options.getInitialSessionCacheMode();
		tenantIdentifier = sessionFactory.resolveTenantIdentifier();
		connectionHandlingMode = options.getPhysicalConnectionHandlingMode();
	}

	protected abstract T getThis();

	@Override
	public T connection(Connection connection) {
		this.connection = connection;
		return getThis();
	}

	@Override
	public T connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		this.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret( acquisitionMode, releaseMode );
		return getThis();
	}

	@Override
	public T interceptor(Interceptor interceptor) {
		if ( interceptor == null ) {
			noInterceptor();
		}
		else {
			this.interceptor = interceptor;
			this.explicitNoInterceptor = false;
		}
		return getThis();
	}

	@Override
	public T noInterceptor() {
		this.interceptor = null;
		this.explicitNoInterceptor = true;
		return getThis();
	}

	@Override
	public T tenantIdentifier(Object tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return getThis();
	}

	@Override
	public T readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return getThis();
	}

	@Override
	public T initialCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return getThis();
	}

	@Override
	public T statementInspector(UnaryOperator<String> operator) {
		if ( operator == null ) {
			noStatementInspector();
		}
		else {
			this.statementInspector = operator::apply;
		}
		return getThis();
	}

	@Override
	public T noStatementInspector() {
		this.statementInspector = null;
		return getThis();
	}
}
