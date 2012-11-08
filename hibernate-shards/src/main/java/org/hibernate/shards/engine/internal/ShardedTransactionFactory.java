package org.hibernate.shards.engine.internal;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.shards.ShardedTransaction;

/**
 * Factory for {@link ShardedTransactionFactory} instances.
 *
 * @author Adriano Machado
 */
public class ShardedTransactionFactory implements TransactionFactory<ShardedTransaction> {

    @Override
    public ShardedTransaction createTransaction(TransactionCoordinator coordinator) {
        // TODO: fix me.
        return new ShardedTransactionImpl(null, coordinator);
    }

    @Override
    public boolean canBeDriver() {
        return true;
    }

    @Override
    public boolean compatibleWithJtaSynchronization() {
        return false;
    }

    @Override
    public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, ShardedTransaction transaction) {
        return false;
    }

    @Override
    public ConnectionReleaseMode getDefaultReleaseMode() {
        return ConnectionReleaseMode.ON_CLOSE;
    }
}
