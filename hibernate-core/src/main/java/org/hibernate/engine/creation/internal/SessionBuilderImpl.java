/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.SessionOwnerBehavior;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static java.util.Collections.addAll;

/**
 * SessionBuilder implementation.
 *
 * @author Steve Ebersole
 */
public class SessionBuilderImpl
		extends AbstractCommonBuilder<SessionBuilderImplementor>
		implements SessionBuilderImplementor, SessionCreationOptions {
	private static final Logger log = CoreLogging.logger( SessionBuilderImpl.class );

	private boolean autoJoinTransactions = true;
	private FlushMode flushMode;
	private boolean autoClose;
	private boolean autoClear;
	private boolean identifierRollback;
	private TimeZone jdbcTimeZone;
	private final int defaultBatchFetchSize;
	private final boolean subselectFetchEnabled;

	// Lazy: defaults can be built by invoking the builder in fastSessionServices.defaultSessionEventListeners
	// (Need a fresh build for each Session as the listener instances can't be reused across sessions)
	// Only initialize of the builder is overriding the default.
	private List<SessionEventListener> listeners;

	//todo : expose setting
	private final SessionOwnerBehavior sessionOwnerBehavior = SessionOwnerBehavior.LEGACY_NATIVE;

	public SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
		super( sessionFactory );

		// set up default builder values...
		final var options = sessionFactory.getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		connectionHandlingMode = options.getPhysicalConnectionHandlingMode();
		autoClose = options.isAutoCloseSessionEnabled();
		defaultBatchFetchSize = options.getDefaultBatchFetchSize();
		subselectFetchEnabled = options.isSubselectFetchEnabled();
		identifierRollback = options.isIdentifierRollbackEnabled();
		cacheMode = options.getInitialSessionCacheMode();

		tenantIdentifier = sessionFactory.resolveTenantIdentifier();
		jdbcTimeZone = options.getJdbcTimeZone();
	}

	@Override
	protected SessionBuilderImplementor getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionCreationOptions


	@Override
	public boolean shouldAutoJoinTransactions() {
		return autoJoinTransactions;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return flushMode;
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		return subselectFetchEnabled;
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	@Override
	public boolean shouldAutoClose() {
		return autoClose;
	}

	@Override
	public boolean shouldAutoClear() {
		return autoClear;
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
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier != null
				? sessionFactory.getTenantIdentifierJavaType().toString( tenantIdentifier )
				: null;
	}

	@Override
	public Object getTenantIdentifierValue() {
		return tenantIdentifier;
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
	public boolean isIdentifierRollbackEnabled() {
		return identifierRollback;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return jdbcTimeZone;
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListener() {
		return listeners;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionBuilder

	@Override
	public SessionImpl openSession() {
		log.tracef( "Opening Hibernate Session.  tenant=%s", tenantIdentifier );
		return new SessionImpl( sessionFactory, this );
	}

	@Override
	@Deprecated
	public SessionBuilderImplementor statementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
		return this;
	}

	@Override
	@Deprecated
	public SessionBuilderImplementor connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		this.connectionHandlingMode = connectionHandlingMode;
		return this;
	}

	@Override
	public SessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		this.autoJoinTransactions = autoJoinTransactions;
		return this;
	}

	@Override
	public SessionBuilderImplementor autoClose(boolean autoClose) {
		this.autoClose = autoClose;
		return this;
	}

	@Override
	public SessionBuilderImplementor autoClear(boolean autoClear) {
		this.autoClear = autoClear;
		return this;
	}

	@Override
	public SessionBuilderImplementor flushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	@Deprecated(forRemoval = true)
	public SessionBuilderImplementor tenantIdentifier(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return this;
	}

	@Override
	public SessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		this.identifierRollback = identifierRollback;
		return this;
	}

	@Override
	public SessionBuilderImplementor eventListeners(SessionEventListener... listeners) {
		if ( this.listeners == null ) {
			final var baselineListeners =
					sessionFactory.getSessionFactoryOptions().buildSessionEventListeners();
			this.listeners = new ArrayList<>( baselineListeners.length + listeners.length );
			addAll( this.listeners, baselineListeners );
		}
		addAll( this.listeners, listeners );
		return this;
	}

	@Override
	public SessionBuilderImplementor clearEventListeners() {
		if ( listeners == null ) {
			//Needs to initialize explicitly to an empty list as otherwise "null" implies the default listeners will be applied
			listeners = new ArrayList<>( 3 );
		}
		else {
			listeners.clear();
		}
		return this;
	}

	@Override
	public SessionBuilderImplementor jdbcTimeZone(TimeZone timeZone) {
		jdbcTimeZone = timeZone;
		return this;
	}


}
