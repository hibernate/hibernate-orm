/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * SimpleJtaTransactionImpl variant that works with DualNodeTransactionManagerImpl.
 *
 * TODO: Merge with single node transaction manager
 * 
 * @author Brian Stansberry
 */
public class DualNodeJtaTransactionImpl implements Transaction {
   private static final Log log = LogFactory.getLog(DualNodeJtaTransactionImpl.class);

   private int status;
   private LinkedList synchronizations;
   private Connection connection; // the only resource we care about is jdbc connection
   private final DualNodeJtaTransactionManagerImpl jtaTransactionManager;
   private List<XAResource> enlistedResources = new ArrayList<XAResource>();
   private Xid xid = new DualNodeJtaTransactionXid();

   public DualNodeJtaTransactionImpl(DualNodeJtaTransactionManagerImpl jtaTransactionManager) {
      this.jtaTransactionManager = jtaTransactionManager;
      this.status = Status.STATUS_ACTIVE;
   }

   public int getStatus() {
      return status;
   }

   public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, IllegalStateException, SystemException {

      if (status == Status.STATUS_MARKED_ROLLBACK) {
         log.trace("on commit, status was marked for rollback-only");
         rollback();
      } else {
         status = Status.STATUS_PREPARING;

         for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization s = (Synchronization) synchronizations.get(i);
            s.beforeCompletion();
         }

         if (!runXaResourcePrepare()) {
            status = Status.STATUS_ROLLING_BACK;
         } else {
            status = Status.STATUS_PREPARED;
         }

         status = Status.STATUS_COMMITTING;

         if (connection != null) {
            try {
               connection.commit();
               connection.close();
            } catch (SQLException sqle) {
               status = Status.STATUS_UNKNOWN;
               throw new SystemException();
            }
         }

         runXaResourceCommitTx();

         status = Status.STATUS_COMMITTED;

         for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization s = (Synchronization) synchronizations.get(i);
            s.afterCompletion(status);
         }

         // status = Status.STATUS_NO_TRANSACTION;
         jtaTransactionManager.endCurrent(this);
      }
   }

   public void rollback() throws IllegalStateException, SystemException {
      status = Status.STATUS_ROLLING_BACK;
      runXaResourceRollback();
      status = Status.STATUS_ROLLEDBACK;

      if (connection != null) {
         try {
            connection.rollback();
            connection.close();
         } catch (SQLException sqle) {
            status = Status.STATUS_UNKNOWN;
            throw new SystemException();
         }
      }

      if (synchronizations != null) {
         for (int i = 0; i < synchronizations.size(); i++) {
            Synchronization s = (Synchronization) synchronizations.get(i);
            s.afterCompletion(status);
         }
      }

      // status = Status.STATUS_NO_TRANSACTION;
      jtaTransactionManager.endCurrent(this);
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException {
      status = Status.STATUS_MARKED_ROLLBACK;
   }

   public void registerSynchronization(Synchronization synchronization) throws RollbackException,
            IllegalStateException, SystemException {
      // todo : find the spec-allowable statuses during which synch can be registered...
      if (synchronizations == null) {
         synchronizations = new LinkedList();
      }
      synchronizations.add(synchronization);
   }

   public void enlistConnection(Connection connection) {
      if (this.connection != null) {
         throw new IllegalStateException("Connection already registered");
      }
      this.connection = connection;
   }

   public Connection getEnlistedConnection() {
      return connection;
   }

   public boolean enlistResource(XAResource xaResource) throws RollbackException,
            IllegalStateException, SystemException {
      enlistedResources.add(new WrappedXaResource(xaResource));
      try {
         xaResource.start(xid, 0);
      } catch (XAException e) {
         log.error("Got an exception", e);
         throw new SystemException(e.getMessage());
      }
      return true;
   }

   public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException,
            SystemException {
      throw new SystemException("not supported");
   }

   public Collection<XAResource> getEnlistedResources() {
      return enlistedResources;
   }

   private boolean runXaResourcePrepare() throws SystemException {
      Collection<XAResource> resources = getEnlistedResources();
      for (XAResource res : resources) {
         try {
            res.prepare(xid);
         } catch (XAException e) {
            log.trace("The resource wants to rollback!", e);
            return false;
         } catch (Throwable th) {
            log.error("Unexpected error from resource manager!", th);
            throw new SystemException(th.getMessage());
         }
      }
      return true;
   }

   private void runXaResourceRollback() {
      Collection<XAResource> resources = getEnlistedResources();
      for (XAResource res : resources) {
         try {
            res.rollback(xid);
         } catch (XAException e) {
            log.warn("Error while rolling back",e);
         }
      }
   }

   private boolean runXaResourceCommitTx() throws HeuristicMixedException {
      Collection<XAResource> resources = getEnlistedResources();
      for (XAResource res : resources) {
         try {
            res.commit(xid, false);//todo we only support one phase commit for now, change this!!!
         } catch (XAException e) {
            log.warn("exception while committing",e);
            throw new HeuristicMixedException(e.getMessage());
         }
      }
      return true;
   }

   private static class DualNodeJtaTransactionXid implements Xid {
      private static AtomicInteger txIdCounter = new AtomicInteger(0);
      private int id = txIdCounter.incrementAndGet();

      public int getFormatId() {
         return id;
      }

      public byte[] getGlobalTransactionId() {
         throw new IllegalStateException("TODO - please implement me!!!"); //todo implement!!!
      }

      public byte[] getBranchQualifier() {
         throw new IllegalStateException("TODO - please implement me!!!"); //todo implement!!!
      }

      @Override
      public String toString() {
         return getClass().getSimpleName() + "{" +
               "id=" + id +
               '}';
      }
   }

   private class WrappedXaResource implements XAResource {
      private final XAResource xaResource;
      private int prepareResult;

      public WrappedXaResource(XAResource xaResource) {
         this.xaResource = xaResource;
      }

      @Override
      public void commit(Xid xid, boolean b) throws XAException {
         // Commit only if not read only.
         if (prepareResult != XAResource.XA_RDONLY)
            xaResource.commit(xid, b);
         else
            log.tracef("Not committing {0} due to readonly.", xid);
      }

      @Override
      public void end(Xid xid, int i) throws XAException {
         xaResource.end(xid, i);
      }

      @Override
      public void forget(Xid xid) throws XAException {
         xaResource.forget(xid);
      }

      @Override
      public int getTransactionTimeout() throws XAException {
         return xaResource.getTransactionTimeout();
      }

      @Override
      public boolean isSameRM(XAResource xaResource) throws XAException {
         return xaResource.isSameRM(xaResource);
      }

      @Override
      public int prepare(Xid xid) throws XAException {
         prepareResult = xaResource.prepare(xid);
         return prepareResult;
      }

      @Override
      public Xid[] recover(int i) throws XAException {
         return xaResource.recover(i);
      }

      @Override
      public void rollback(Xid xid) throws XAException {
         xaResource.rollback(xid);
      }

      @Override
      public boolean setTransactionTimeout(int i) throws XAException {
         return xaResource.setTransactionTimeout(i);
      }

      @Override
      public void start(Xid xid, int i) throws XAException {
         xaResource.start(xid, i);
      }
   }
}
