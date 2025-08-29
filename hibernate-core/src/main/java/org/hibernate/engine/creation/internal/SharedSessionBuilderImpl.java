/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.engine.creation.spi.SharedSessionBuilderImplementor;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * @author Steve Ebersole
 */
public class SharedSessionBuilderImpl
		extends SessionBuilderImpl
		implements SharedSessionBuilderImplementor, SharedSessionCreationOptions {
	private final SessionImpl session;

	private boolean shareTransactionContext;
	private boolean tenantIdChanged;
	private boolean readOnlyChanged;

	public SharedSessionBuilderImpl(SessionImpl session) {
		super( (SessionFactoryImpl) session.getFactory() );
		this.session = session;
		super.tenantIdentifier( session.getTenantIdentifierValue() );
		super.identifierRollback( session.isIdentifierRollbackEnabled() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionBuilder

	@Override
	public SessionImpl openSession() {
		if ( session.getFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
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
		return super.openSession();
	}


	@Override
	@Deprecated(forRemoval = true)
	public SharedSessionBuilderImplementor tenantIdentifier(String tenantIdentifier) {
		tenantIdentifier( (Object) tenantIdentifier );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor tenantIdentifier(Object tenantIdentifier) {
		super.tenantIdentifier( tenantIdentifier );
		tenantIdChanged = true;
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor readOnly(boolean readOnly) {
		super.readOnly( readOnly );
		readOnlyChanged = true;
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor connection() {
		this.shareTransactionContext = true;
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor interceptor() {
		interceptor = session.getInterceptor();
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor statementInspector() {
		statementInspector = session.getJdbcSessionContext().getStatementInspector();
		return this;
	}

	private PhysicalConnectionHandlingMode getConnectionHandlingMode() {
		return session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode();
	}

	@Override
	@Deprecated(since = "6.0")
	public SharedSessionBuilderImplementor connectionReleaseMode() {
		final PhysicalConnectionHandlingMode handlingMode =
				PhysicalConnectionHandlingMode.interpret( ConnectionAcquisitionMode.AS_NEEDED,
						getConnectionHandlingMode().getReleaseMode() );
		connectionHandlingMode( handlingMode );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor connectionHandlingMode() {
		connectionHandlingMode( getConnectionHandlingMode() );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor autoJoinTransactions() {
		super.autoJoinTransactions( session.shouldAutoJoinTransaction() );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		super.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor autoClose(boolean autoClose) {
		super.autoClose( autoClose );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor flushMode() {
		flushMode( session.getHibernateFlushMode() );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor autoClose() {
		autoClose( session.isAutoCloseSessionEnabled() );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		super.identifierRollback( identifierRollback );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor jdbcTimeZone(TimeZone timeZone) {
		super.jdbcTimeZone( timeZone );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor clearEventListeners() {
		super.clearEventListeners();
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor flushMode(FlushMode flushMode) {
		super.flushMode( flushMode );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor autoClear(boolean autoClear) {
		super.autoClear( autoClear );
		return this;
	}

	@Override
	@Deprecated
	public SharedSessionBuilderImplementor statementInspector(StatementInspector statementInspector) {
		super.statementInspector( statementInspector );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor statementInspector(UnaryOperator<String> operator) {
		super.statementInspector( operator );
		return this;
	}

	@Override
	@Deprecated
	public SharedSessionBuilderImplementor connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		super.connectionHandlingMode( connectionHandlingMode );
		return this;
	}

	@Override
	@Deprecated
	public SharedSessionBuilderImplementor connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		super.connectionHandling( acquisitionMode, releaseMode );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor eventListeners(SessionEventListener... listeners) {
		super.eventListeners( listeners );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor connection(Connection connection) {
		super.connection( connection );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor interceptor(Interceptor interceptor) {
		super.interceptor( interceptor );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor noInterceptor() {
		super.noInterceptor();
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor initialCacheMode(CacheMode cacheMode) {
		super.initialCacheMode( cacheMode );
		return this;
	}

	@Override
	public SharedSessionBuilderImplementor noStatementInspector() {
		super.noStatementInspector();
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionCreationOptions

	@Override
	public boolean isTransactionCoordinatorShared() {
		return shareTransactionContext;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return shareTransactionContext ? session.getTransactionCoordinator() : null;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return shareTransactionContext ? session.getJdbcCoordinator() : null;
	}

	@Override
	public Transaction getTransaction() {
		return shareTransactionContext ? session.getCurrentTransaction() : null;
	}

	@Override
	public TransactionCompletionCallbacksImpl getTransactionCompletionCallbacks() {
		return shareTransactionContext
				? session.getActionQueue().getTransactionCompletionCallbacks()
				: null;
	}
}
