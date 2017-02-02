/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Hashtable;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;

/**
 * Variant of SimpleJtaTransactionManagerImpl that doesn't use a VM-singleton, but rather a set of
 * impls keyed by a node id.
 *
 * TODO: Merge with single node transaction manager as much as possible
 *
 * @author Brian Stansberry
 */
public class DualNodeJtaTransactionManagerImpl implements TransactionManager {

   private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(DualNodeJtaTransactionManagerImpl.class);

   private static final Hashtable INSTANCES = new Hashtable();

   private ThreadLocal currentTransaction = new ThreadLocal();
   private String nodeId;

   public synchronized static DualNodeJtaTransactionManagerImpl getInstance(String nodeId) {
      DualNodeJtaTransactionManagerImpl tm = (DualNodeJtaTransactionManagerImpl) INSTANCES
               .get(nodeId);
      if (tm == null) {
         tm = new DualNodeJtaTransactionManagerImpl(nodeId);
         INSTANCES.put(nodeId, tm);
      }
      return tm;
   }

   public synchronized static void cleanupTransactions() {
      for (java.util.Iterator it = INSTANCES.values().iterator(); it.hasNext();) {
         TransactionManager tm = (TransactionManager) it.next();
         try {
            tm.suspend();
         } catch (Exception e) {
            log.error("Exception cleaning up TransactionManager " + tm);
         }
      }
   }

   public synchronized static void cleanupTransactionManagers() {
      INSTANCES.clear();
   }

   private DualNodeJtaTransactionManagerImpl(String nodeId) {
      this.nodeId = nodeId;
   }

   public int getStatus() throws SystemException {
      Transaction tx = getCurrentTransaction();
      return tx == null ? Status.STATUS_NO_TRANSACTION : tx.getStatus();
   }

   public Transaction getTransaction() throws SystemException {
      return (Transaction) currentTransaction.get();
   }

   public DualNodeJtaTransactionImpl getCurrentTransaction() {
      return (DualNodeJtaTransactionImpl) currentTransaction.get();
   }

   public void begin() throws NotSupportedException, SystemException {
      currentTransaction.set(new DualNodeJtaTransactionImpl(this));
   }

   public Transaction suspend() throws SystemException {
      DualNodeJtaTransactionImpl suspended = getCurrentTransaction();
      log.trace(nodeId + ": Suspending " + suspended + " for thread "
               + Thread.currentThread().getName());
      currentTransaction.set(null);
      return suspended;
   }

   public void resume(Transaction transaction) throws InvalidTransactionException,
            IllegalStateException, SystemException {
      currentTransaction.set(transaction);
      log.trace(nodeId + ": Resumed " + transaction + " for thread "
               + Thread.currentThread().getName());
   }

   public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
      Transaction tx = getCurrentTransaction();
      if (tx == null) {
         throw new IllegalStateException("no current transaction to commit");
      }
      tx.commit();
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException {
      Transaction tx = getCurrentTransaction();
      if (tx == null) {
         throw new IllegalStateException("no current transaction");
      }
      tx.rollback();
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException {
      Transaction tx = getCurrentTransaction();
      if (tx == null) {
         throw new IllegalStateException("no current transaction");
      }
      tx.setRollbackOnly();
   }

   public void setTransactionTimeout(int i) throws SystemException {
   }

   void endCurrent(DualNodeJtaTransactionImpl transaction) {
      if (transaction == currentTransaction.get()) {
         currentTransaction.set(null);
      }
   }

   @Override
public String toString() {
      StringBuffer sb = new StringBuffer(getClass().getName());
      sb.append("[nodeId=");
      sb.append(nodeId);
      sb.append("]");
      return sb.toString();
   }
}
