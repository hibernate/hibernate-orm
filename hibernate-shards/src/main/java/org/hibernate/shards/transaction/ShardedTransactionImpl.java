/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.transaction;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardedTransaction;
import org.hibernate.shards.engine.ShardedSessionImplementor;
import org.hibernate.shards.internal.ShardsMessageLogger;
import org.hibernate.shards.session.OpenSessionEvent;
import org.hibernate.shards.session.SetupTransactionOpenSessionEvent;
import org.jboss.logging.Logger;

import javax.transaction.SystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tomislav Nad
 * @author Adriano Machado
 */
public class ShardedTransactionImpl extends AbstractTransactionImpl implements ShardedTransaction {

    public static final ShardsMessageLogger LOG = Logger.getMessageLogger(ShardsMessageLogger.class, ShardedTransactionImpl.class.getName());

    private final List<TransactionImplementor> transactions;

    private boolean begun;
    private boolean commitFailed;
    private boolean timeoutSet;
    private int timeout;
    private boolean initiator;

    public ShardedTransactionImpl(final ShardedSessionImplementor ssi,
                                  final TransactionCoordinator transactionCoordinator) {

        super(transactionCoordinator);

        final OpenSessionEvent osEvent = new SetupTransactionOpenSessionEvent(this);
        transactions = Collections.synchronizedList(new ArrayList<TransactionImplementor>());
        for (final Shard shard : ssi.getShards()) {
            if (shard.getSession() != null) {
                final TransactionImplementor transaction = (TransactionImplementor)shard.getSession().getTransaction();
                transactions.add(transaction);
                if (!initiator && transaction.isInitiator()) {
                    this.initiator = true;
                }
            } else {
                shard.addOpenSessionEvent(osEvent);
            }
        }
    }

    @Override
    public void setupTransaction(final Session session) {

        LOG.debug("Setting up transaction");

        transactions.add((TransactionImplementor)session.getTransaction());

        if (begun) {
            session.beginTransaction();
        }

        if (timeoutSet) {
            session.getTransaction().setTimeout(timeout);
        }
    }

    @Override
    public boolean isInitiator() {
        return initiator;
    }

    @Override
    public void setTimeout(final int seconds) {
        timeoutSet = true;
        timeout = seconds;
        for (final Transaction t : transactions) {
            t.setTimeout(timeout);
        }
    }

    @Override
    protected void doBegin() {

        boolean beginException = false;
        for (final Transaction t : transactions) {
            try {
                t.begin();
            } catch (HibernateException he) {
                LOG.warn("exception starting underlying transaction", he);
                beginException = true;
            }
        }

        if (beginException) {
            for (final Transaction t : transactions) {
                if (t.isActive()) {
                    try {
                        t.rollback();
                    } catch (HibernateException he) {
                        LOG.fatal("Unable to rollback sharded transaction.");
                        // TODO(maxr) What do we do?
                    }
                }
            }
            throw new TransactionException("Begin failed");
        }
    }

    @Override
    protected void doCommit() {
        LOG.debug("Starting transaction commit");

        boolean commitException = false;
        HibernateException firstCommitException = null;
        for (Transaction t : transactions) {
            try {
                t.commit();
            } catch (HibernateException he) {
                LOG.warn("exception commiting underlying transaction", he);
                commitException = true;
                // we're only going to rethrow the first commit exception we receive
                if (firstCommitException == null) {
                    firstCommitException = he;
                }
            }
        }

        if (commitException) {
            throw firstCommitException;
        }
    }

    @Override
    protected void doRollback() {
        if (!begun && !commitFailed) {
            throw new TransactionException("Transaction not successfully started");
        }

        boolean rollbackException = false;
        HibernateException firstRollbackException = null;
        for (final Transaction t : transactions) {
            if (t.wasCommitted()) {
                continue;
            }
            try {
                t.rollback();
            } catch (HibernateException he) {
                LOG.warn("exception rolling back underlying transaction", he);
                rollbackException = true;
                if (firstRollbackException == null) {
                    firstRollbackException = he;
                }
            }
        }

        if (rollbackException) {
            // we're only going to rethrow the first rollback exception
            throw new TransactionException("Rollback failed", firstRollbackException);
        }
    }

    @Override
    protected void afterTransactionBegin() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void beforeTransactionCommit() {
        transactionCoordinator().getSynchronizationRegistry().notifySynchronizationsBeforeTransactionCompletion();
    }

    @Override
    protected void beforeTransactionRollBack() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void afterTransactionCompletion(int status) {
        begun = false;
        transactionCoordinator().getSynchronizationRegistry().notifySynchronizationsAfterTransactionCompletion(status);
    }

    @Override
    protected void afterAfterCompletion() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IsolationDelegate createIsolationDelegate() {
        return new ShardsIsolationDelegate(transactionCoordinator());
    }

    @Override
    public JoinStatus getJoinStatus() {
        return isActive() ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
    }

    @Override
    public void markRollbackOnly() {
        for (final TransactionImplementor t : transactions) {
            try {
                t.markRollbackOnly();
            } catch (HibernateException he) {
                LOG.warn("exception on mark rollback only on underlying transaction", he);
            }
        }
    }

    @Override
    public void markForJoin() {
        for (final TransactionImplementor t : transactions) {
            try {
                t.markForJoin();
            } catch (HibernateException he) {
                LOG.warn("exception on mark rollback only on underlying transaction", he);
            }
        }
    }

    @Override
    public void join() {
        for (final TransactionImplementor t : transactions) {
            try {
                t.join();
            } catch (HibernateException he) {
                LOG.warn("exception on mark rollback only on underlying transaction", he);
            }
        }
    }

    @Override
    public void resetJoinStatus() {
        for (final TransactionImplementor t : transactions) {
            try {
                t.resetJoinStatus();
            } catch (HibernateException he) {
                LOG.warn("exception on mark rollback only on underlying transaction", he);
            }
        }
    }
}
