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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardedTransaction;
import org.hibernate.shards.engine.ShardedSessionImplementor;
import org.hibernate.shards.session.OpenSessionEvent;
import org.hibernate.shards.session.SetupTransactionOpenSessionEvent;
import org.hibernate.shards.util.Lists;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tomislav Nad
 */
public class ShardedTransactionImpl implements ShardedTransaction {

  private final Log log = LogFactory.getLog(getClass());

  private final List<Transaction> transactions;

  private boolean begun;
  private boolean rolledBack;
  private boolean committed;
  private boolean commitFailed;
  private List<Synchronization> synchronizations;
  private boolean timeoutSet;
  private int timeout;


  public ShardedTransactionImpl(ShardedSessionImplementor ssi) {
    OpenSessionEvent osEvent = new SetupTransactionOpenSessionEvent(this);
    transactions = Collections.synchronizedList(new ArrayList<Transaction>());
    for(Shard shard : ssi.getShards()) {
      if (shard.getSession() != null) {
        transactions.add(shard.getSession().getTransaction());
      } else {
        shard.addOpenSessionEvent(osEvent);
      }
    }
  }

  public void setupTransaction(Session session) {
    log.debug("Setting up transaction");
    transactions.add(session.getTransaction());
    if (begun) {
     session.beginTransaction();
    }
    if (timeoutSet) {
      session.getTransaction().setTimeout(timeout);
    }
  }

  public void begin() throws HibernateException {
    if (begun) {
      return;
    }
    if (commitFailed) {
      throw new TransactionException("cannot re-start transaction after failed commit");
    }
    boolean beginException = false;
    for (Transaction t : transactions) {
      try {
        t.begin();
      } catch (HibernateException he) {
        log.warn("exception starting underlying transaction", he);
        beginException = true;
      }
    }
    if (beginException) {
      for (Transaction t : transactions) {
        if (t.isActive()) {
          try {
            t.rollback();
          } catch (HibernateException he) {
            // TODO(maxr) What do we do?
          }

        }
      }
      throw new TransactionException("Begin failed");
    }
    begun = true;
    committed = false;
    rolledBack = false;
  }

  public void commit() throws HibernateException {
    if (!begun) {
      throw new TransactionException("Transaction not succesfully started");
    }
    log.debug("Starting transaction commit");
    beforeTransactionCompletion();
    boolean commitException = false;
    HibernateException firstCommitException = null;
    for(Transaction t : transactions) {
      try {
        t.commit();
      } catch (HibernateException he) {
        log.warn("exception commiting underlying transaction", he);
        commitException = true;
        // we're only going to rethrow the first commit exception we receive
        if(firstCommitException == null) {
          firstCommitException = he;
        }
      }
    }
    if (commitException) {
      commitFailed  = true;
      afterTransactionCompletion(Status.STATUS_UNKNOWN);
      throw new TransactionException("Commit failed", firstCommitException);
    }
    afterTransactionCompletion(Status.STATUS_COMMITTED);
    committed = true;
  }

  public void rollback() throws HibernateException {
    if (!begun && !commitFailed) {
      throw new TransactionException("Transaction not successfully started");
    }
    boolean rollbackException = false;
    HibernateException firstRollbackException = null;
    for(Transaction t : transactions) {
      if (t.wasCommitted()) {
        continue;
      }
      try {
        t.rollback();
      } catch (HibernateException he) {
        log.warn("exception rolling back underlying transaction", he);
        rollbackException = true;
        if(firstRollbackException == null) {
          firstRollbackException = he;
        }
      }
    }
    if (rollbackException) {
      // we're only going to rethrow the first rollback exception
      throw new TransactionException("Rollback failed", firstRollbackException);
    }
    rolledBack = true;
  }

  public boolean wasRolledBack() throws HibernateException {
    return rolledBack;
  }

  public boolean wasCommitted() throws HibernateException {
    return committed;
  }

  public boolean isActive() throws HibernateException {
    return begun && !(rolledBack || committed || commitFailed);
  }

  public void registerSynchronization(Synchronization sync)
      throws HibernateException {
    if (sync == null) {
      throw new NullPointerException("null Synchronization");
    }
    if (synchronizations == null) {
      synchronizations = Lists.newArrayList();
    }
    synchronizations.add(sync);
  }

  public void setTimeout(int seconds) {
    timeoutSet = true;
    timeout = seconds;
    for(Transaction t : transactions) {
      t.setTimeout(timeout);
    }
  }

  private void beforeTransactionCompletion() {
    if (synchronizations != null) {
      for (Synchronization sync : synchronizations) {
        try {
          sync.beforeCompletion();
        } catch (Throwable t) {
          log.warn("exception calling user Synchronization", t);
        }
      }
    }
  }

  private void afterTransactionCompletion(int status) {
    begun = false;
    if (synchronizations != null) {
      for (Synchronization sync : synchronizations) {
        try {
          sync.afterCompletion(status);
        } catch (Throwable t) {
          log.warn("exception calling user Synchronization", t);
        }
      }
    }
  }
}
