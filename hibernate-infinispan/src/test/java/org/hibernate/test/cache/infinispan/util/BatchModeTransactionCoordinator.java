package org.hibernate.test.cache.infinispan.util;

import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.SynchronizationRegistry;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.infinispan.transaction.tm.BatchModeTransactionManager;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

/**
 * Mocks transaction coordinator when {@link org.hibernate.engine.spi.SessionImplementor} is only mocked
 * and {@link org.infinispan.transaction.tm.BatchModeTransactionManager} is used.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class BatchModeTransactionCoordinator implements TransactionCoordinator {
   @Override
   public void explicitJoin() {
   }

   @Override
   public boolean isJoined() {
      return true;
   }

   @Override
   public void pulse() {
   }

   @Override
   public TransactionDriver getTransactionDriverControl() {
      throw new UnsupportedOperationException();
   }

   @Override
   public SynchronizationRegistry getLocalSynchronizations() {
      return new SynchronizationRegistry() {
         @Override
         public void registerSynchronization(Synchronization synchronization) {
            try {
               BatchModeTransactionManager.getInstance().getTransaction().registerSynchronization(synchronization);
            } catch (RollbackException e) {
               throw new RuntimeException(e);
            } catch (SystemException e) {
               throw new RuntimeException(e);
            }
         }
      };
   }

   @Override
   public boolean isActive() {
      try {
         return BatchModeTransactionManager.getInstance().getStatus() == Status.STATUS_ACTIVE;
      } catch (SystemException e) {
         return false;
      }
   }

   @Override
   public IsolationDelegate createIsolationDelegate() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void addObserver(TransactionObserver observer) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void removeObserver(TransactionObserver observer) {
      throw new UnsupportedOperationException();
   }

   @Override
   public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setTimeOut(int seconds) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getTimeOut() {
      throw new UnsupportedOperationException();
   }
}
