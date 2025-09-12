package org.hibernate.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.Transaction;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.creation.internal.SharedSessionCreationOptions;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

/**
 * Wraps a {@link CommonSharedSessionCreationOptions} as a
 * {@link SharedSessionCreationOptions} to pass to
 * {@link org.hibernate.internal.AbstractSharedSessionContract}
 * during construction.
 *
 * @param factory The {@code SessionFactoryImplementor}
 * @param options The {@code CommonSharedSessionCreationOptions} being wrapped.
 */
public record SessionCreationOptionsAdaptor(
		SessionFactoryImplementor factory,
		CommonSharedSessionCreationOptions options)
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
		return null;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		// identifier rollback not yet implemented for StatelessSessions
		return false;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return factory.getSessionFactoryOptions().getPhysicalConnectionHandlingMode();
	}

	@Override
	public String getTenantIdentifier() {
		final Object tenantIdentifier = getTenantIdentifierValue();
		return tenantIdentifier == null
				? null
				: factory.getTenantIdentifierJavaType().toString( tenantIdentifier );
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return factory.getSessionFactoryOptions().getJdbcTimeZone();
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListener() {
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
	public TransactionCompletionCallbacksImpl getTransactionCompletionCallbacks() {
		return null;
	}
}
