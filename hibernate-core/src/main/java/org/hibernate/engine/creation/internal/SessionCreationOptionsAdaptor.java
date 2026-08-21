/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

/**
 * Wraps a {@link CommonSharedSessionCreationOptions} as a
 * {@link SharedSessionCreationOptions} to pass to
 * {@code org.hibernate.internal.AbstractSharedSessionContract}
 * during construction.
 *
 * @param factory The {@code SessionFactoryImplementor}
 * @param options The {@code CommonSharedSessionCreationOptions} being wrapped.
 */
public record SessionCreationOptionsAdaptor(
		SessionFactoryImplementor factory,
		CommonSharedSessionCreationOptions options,
		SharedSessionContractImplementor originalSession)
			implements SharedSessionCreationOptions {

	@Override
	public Interceptor getInterceptor() {
		return options.getInterceptor();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return options.getStatementInspector();
	}

	@Override
	public Object getTenantIdentifierValue() {
		return options.getTenantIdentifierValue();
	}

	@Override
	public boolean isReadOnly() {
		return options.isReadOnly();
	}

	@Override
	public CacheMode getInitialCacheMode() {
		return options.getInitialCacheMode();
	}

	@Override
	public boolean shouldAutoJoinTransactions() {
		return true;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		// stateless sessions don't have a flush mode
		return FlushMode.AUTO;
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		// for some reason, StatelessSession has no setSubselectFetchEnabled()
		return false;
	}

	@Override
	public int getDefaultBatchFetchSize() {
		// for some reason, StatelessSession has no setFetchBatchSize()
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
		return null;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		// identifier rollback not yet implemented for StatelessSessions
		return false;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return options.getPhysicalConnectionHandlingMode();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		// not exposed via SharedStatelessSessionBuilder
		return options.getJdbcTimeZone();
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListeners() {
		return null;
	}

	@Override
	public boolean isTransactionCoordinatorShared() {
		return options.isTransactionCoordinatorShared();
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return options.getTransactionCoordinator();
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return options.getJdbcCoordinator();
	}

	@Override
	public Transaction getTransaction() {
		return options.getTransaction();
	}

	@Override
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks() {
		return options.getTransactionCompletionCallbacksImplementor();
	}

	@Override
	public void registerParentSessionObserver(ParentSessionObserver observer) {
		registerParentSessionObserver( observer, originalSession );
	}
}
