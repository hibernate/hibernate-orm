/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.tm;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * XaResourceCapableTransactionManagerImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class XaTransactionManagerImpl implements TransactionManager {
   private static final XaTransactionManagerImpl INSTANCE = new XaTransactionManagerImpl();
   private final ThreadLocal<XaTransactionImpl> currentTransaction = new ThreadLocal<>();

   public static XaTransactionManagerImpl getInstance() {
      return INSTANCE;
   }

   public int getStatus() throws SystemException {
      XaTransactionImpl currentTransaction = this.currentTransaction.get();
      return currentTransaction == null ? Status.STATUS_NO_TRANSACTION : currentTransaction.getStatus();
   }

   public Transaction getTransaction() throws SystemException {
      return currentTransaction.get();
   }

   public XaTransactionImpl getCurrentTransaction() {
      return currentTransaction.get();
   }

   public void begin() throws NotSupportedException, SystemException {
      if (currentTransaction.get() != null) throw new IllegalStateException("Transaction already started.");
      currentTransaction.set(new XaTransactionImpl(this));
   }

   public Transaction suspend() throws SystemException {
      Transaction suspended = currentTransaction.get();
      currentTransaction.remove();
      return suspended;
   }

   public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException,
            SystemException {
      currentTransaction.set((XaTransactionImpl) transaction);
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
            SecurityException, IllegalStateException, SystemException {
      XaTransactionImpl currentTransaction = this.currentTransaction.get();
      if (currentTransaction == null) {
         throw new IllegalStateException("no current transaction to commit");
      }
      currentTransaction.commit();
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException {
      XaTransactionImpl currentTransaction = this.currentTransaction.get();
      if (currentTransaction == null) {
         throw new IllegalStateException("no current transaction");
      }
      currentTransaction.rollback();
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException {
      XaTransactionImpl currentTransaction = this.currentTransaction.get();
      if (currentTransaction == null) {
         throw new IllegalStateException("no current transaction");
      }
      currentTransaction.setRollbackOnly();
   }

   public void setTransactionTimeout(int i) throws SystemException {
   }

   void endCurrent(Transaction transaction) {
      currentTransaction.remove();
   }
}
