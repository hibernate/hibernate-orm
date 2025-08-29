/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Steve Ebersole
 */
public class StatelessSessionBuilderImpl
		extends AbstractCommonBuilder<StatelessSessionBuilder>
		implements StatelessSessionBuilder, SessionCreationOptions {

	public StatelessSessionBuilderImpl(SessionFactoryImpl sessionFactory) {
		super( sessionFactory );
	}

	@Override
	protected StatelessSessionBuilder getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// StatelessSessionBuilder

	@Override
	public StatelessSession openStatelessSession() {
		return new StatelessSessionImpl( sessionFactory, this );
	}

	@Override
	@Deprecated
	public StatelessSessionBuilder tenantIdentifier(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return this;
	}

	@Override
	@Deprecated
	public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionCreationOptions

	@Override
	public boolean shouldAutoJoinTransactions() {
		return true;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return FlushMode.ALWAYS;
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		return false;
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return -1;
	}

	@Override
	public boolean shouldAutoClose() {
		return false;
	}

	@Override
	public boolean shouldAutoClear() {
		return false;
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public Interceptor getInterceptor() {
		return SessionFactoryImpl.configuredInterceptor( interceptor, explicitNoInterceptor,
				sessionFactory.getSessionFactoryOptions() );
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		// identifier rollback is not yet implemented for StatelessSessions
		return false;
	}

	@Override
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier == null ? null
				: sessionFactory.getTenantIdentifierJavaType().toString( tenantIdentifier );
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public CacheMode getInitialCacheMode() {
		return cacheMode;
	}

	@Override
	public Object getTenantIdentifierValue() {
		return tenantIdentifier;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListener() {
		return null;
	}
}
