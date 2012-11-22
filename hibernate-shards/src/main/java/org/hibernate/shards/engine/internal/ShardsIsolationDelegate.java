package org.hibernate.shards.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.jboss.logging.Logger;

/**
 * The isolation delegate for Shards based transactions
 *
 * @author Adriano Machado
 */
public class ShardsIsolationDelegate implements IsolationDelegate {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, ShardsIsolationDelegate.class.getName());

    private final TransactionCoordinator transactionCoordinator;

    public ShardsIsolationDelegate(final TransactionCoordinator transactionCoordinator) {
        this.transactionCoordinator = transactionCoordinator;
    }

    @Override
    public <T> T delegateWork(final WorkExecutorVisitable<T> work, final boolean transacted) throws HibernateException {
        throw new UnsupportedOperationException("review this implementation");
    }
}
