/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.engine.creation.spi.SharedSessionBuilderImplementor;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static java.util.Collections.addAll;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class SharedSessionBuilderImpl
		extends AbstractCommonBuilder<SharedSessionBuilderImplementor>
		implements SharedSessionBuilderImplementor, SharedSessionCreationOptions {

	private final SharedSessionContractImplementor original;

	private boolean shareTransactionContext;
	private boolean autoJoinTransactions = true;
	private boolean autoClose;
	private boolean autoClear;
	private boolean identifierRollback;
	private FlushMode flushMode;
	private int defaultBatchFetchSize;
	private boolean subselectFetchEnabled;

	private boolean tenantIdChanged;
	private boolean readOnlyChanged;


	// Lazy: defaults are built only when overriding the factory-level listeners.
	// Need a fresh build for each Session as the listener instances can't be
	// reused across sessions.
	private List<SessionEventListener> listeners;

	public SharedSessionBuilderImpl(SharedSessionContractImplementor original) {
		super( original.getFactory() );
		this.original = original;
		final var options = sessionFactory.getSessionFactoryOptions();
		autoClose = options.isAutoCloseSessionEnabled();
		identifierRollback = options.isIdentifierRollbackEnabled();
		defaultBatchFetchSize = options.getDefaultBatchFetchSize();
		subselectFetchEnabled = options.isSubselectFetchEnabled();
		// override defaults from factory
		tenantIdentifier = original.getTenantIdentifierValue();
		identifierRollback = original.isIdentifierRollbackEnabled();
		// good idea to inherit this
		jdbcTimeZone = original.getJdbcTimeZone();
		temporalIdentifier = original.getLoadQueryInfluencers().getTemporalIdentifier();
	}

	protected abstract SessionImplementor createSession();

	@Override
	@Nonnull
	protected SharedSessionBuilderImplementor getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionBuilder

	@Override
	@Nonnull
	public SessionImplementor open() {
		CORE_LOGGER.openingSession( tenantIdentifier );
		if ( original.getFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( shareTransactionContext ) {
				if ( tenantIdChanged ) {
					throw new SessionException(
							"Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
				if ( readOnlyChanged ) {
					throw new SessionException(
							"Cannot redefine the read-only mode on a child session if the connection is reused" );
				}
			}
		}
		return createSession();
	}

	@Override
	@Nonnull
	public SessionImplementor openSession() {
		return open();
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor tenantIdentifier(Object tenantIdentifier) {
		super.tenantIdentifier( tenantIdentifier );
		tenantIdChanged = true;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor readOnly(boolean readOnly) {
		super.readOnly( readOnly );
		readOnlyChanged = true;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor connection() {
		shareTransactionContext = true;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor interceptor() {
		interceptor = original.getInterceptor();
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor statementInspector() {
		statementInspector = original.getJdbcSessionContext().getStatementInspector();
		return this;
	}

	private PhysicalConnectionHandlingMode getConnectionHandlingMode() {
		return original.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode();
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor connectionHandlingMode() {
		connectionHandlingMode( getConnectionHandlingMode() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoJoinTransactions() {
		this.autoJoinTransactions = original.shouldAutoJoinTransaction();
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		this.autoJoinTransactions = autoJoinTransactions;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClose(boolean autoClose) {
		this.autoClose = autoClose;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor flushMode() {
		flushMode( original.getHibernateFlushMode() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClose() {
		autoClose( original instanceof SessionImplementor statefulSession
				&& statefulSession.isAutoCloseSessionEnabled() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		this.identifierRollback = identifierRollback;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor clearEventListeners() {
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
	@Nonnull
	public SharedSessionBuilderImplementor flushMode(@Nonnull FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClear(boolean autoClear) {
		this.autoClear = autoClear;
		return this;
	}

	@Override
	@Deprecated
	@Nonnull
	public SharedSessionBuilderImplementor statementInspector(@Nonnull StatementInspector statementInspector) {
		super.statementInspector( statementInspector::inspect );
		return this;
	}

	@Override
	@Deprecated
	@Nonnull
	public SharedSessionBuilderImplementor connectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode connectionHandlingMode) {
		super.connectionHandlingMode = connectionHandlingMode;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor eventListeners(@Nonnull SessionEventListener... listeners) {
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
	@Nonnull
	public SharedSessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize) {
		this.defaultBatchFetchSize = defaultBatchFetchSize;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled) {
		this.subselectFetchEnabled = subselectFetchEnabled;
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionCreationOptions

	@Override
	public void registerParentSessionObserver(ParentSessionObserver observer) {
		registerParentSessionObserver( observer, original );
	}

	@Override
	public boolean isTransactionCoordinatorShared() {
		return shareTransactionContext;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return shareTransactionContext
				? original.getTransactionCoordinator()
				: null;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return shareTransactionContext
				? original.getJdbcCoordinator()
				: null;
	}

	@Override
	public Transaction getTransaction() {
		return shareTransactionContext
				? original.getCurrentTransaction()
				: null;
	}

	@Override
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks() {
		return shareTransactionContext
				? original.getTransactionCompletionCallbacksImplementor()
				: null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CommonSharedSessionCreationOptions

	@Override
	public Interceptor getInterceptor() {
		return configuredInterceptor();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return statementInspector;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionCreationOptions

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
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
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
	public Object getTemporalIdentifier() {
		return temporalIdentifier;
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListeners() {
		return listeners;
	}
}
