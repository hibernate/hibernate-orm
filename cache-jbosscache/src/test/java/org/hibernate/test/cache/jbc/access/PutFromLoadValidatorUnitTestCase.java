/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc.access;

import javax.transaction.TransactionManager;

import org.hibernate.cache.jbc.access.PutFromLoadValidator;
import org.hibernate.test.cache.jbc.functional.util.DualNodeJtaTransactionManagerImpl;

import junit.framework.TestCase;

/**
 * Tests of {@link PutFromLoadValidator}.
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class PutFromLoadValidatorUnitTestCase extends TestCase
{
   private Object KEY1= "KEY1";
   
   private TransactionManager tm;
   
   public PutFromLoadValidatorUnitTestCase(String name) {
      super(name);
   }
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      tm = DualNodeJtaTransactionManagerImpl.getInstance("test");
   }

   @Override
   protected void tearDown() throws Exception
   {
      try {
         super.tearDown();
      }
      finally {
         tm = null;
         try {
            DualNodeJtaTransactionManagerImpl.cleanupTransactions();
         }
         finally {
            DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
         }
      }
   }

   public void testNakedPut() throws Exception  {
      nakedPutTest(false);
   }

   public void testNakedPutTransactional() throws Exception {
      nakedPutTest(true);
   }

   private void nakedPutTest(boolean transactional) throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
      if (transactional) {
         tm.begin();
      }
      assertTrue(testee.isPutValid(KEY1));
   }

   public void testRegisteredPut() throws Exception {
      registeredPutTest(false);
   }

   public void testRegisteredPutTransactional() throws Exception {
      registeredPutTest(true);
   }

   private void registeredPutTest(boolean transactional) throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
      if (transactional) {
         tm.begin();
      }
      testee.registerPendingPut(KEY1);
      assertTrue(testee.isPutValid(KEY1));
   }
   
   public void testNakedPutAfterKeyRemoval() throws Exception {
      nakedPutAfterRemovalTest(false, false);
   }
   
   public void testNakedPutAfterKeyRemovalTransactional() throws Exception {
      nakedPutAfterRemovalTest(true, false);
   }
   
   public void testNakedPutAfterRegionRemoval() throws Exception {
      nakedPutAfterRemovalTest(false, true);
   }
   
   public void testNakedPutAfterRegionRemovalTransactional() throws Exception {
      nakedPutAfterRemovalTest(true, true);
   }

   private void nakedPutAfterRemovalTest(boolean transactional, boolean removeRegion) throws Exception
   {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
      if (removeRegion) {
         testee.regionRemoved();
      }
      else {
         testee.keyRemoved(KEY1);
      }
      if (transactional) {
         tm.begin();
      }
      assertFalse(testee.isPutValid(KEY1));
      
   }
   
   public void testRegisteredPutAfterKeyRemoval() throws Exception {
      registeredPutAfterRemovalTest(false, false);
   }
   
   public void testRegisteredPutAfterKeyRemovalTransactional() throws Exception {
      registeredPutAfterRemovalTest(true, false);
   }
   
   public void testRegisteredPutAfterRegionRemoval() throws Exception {
      registeredPutAfterRemovalTest(false, true);
   }
   
   public void testRegisteredPutAfterRegionRemovalTransactional() throws Exception {
      registeredPutAfterRemovalTest(true, true);
   }

   private void registeredPutAfterRemovalTest(boolean transactional, boolean removeRegion) throws Exception
   {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
      if (removeRegion) {
         testee.regionRemoved();
      }
      else {
         testee.keyRemoved(KEY1);
      }
      if (transactional) {
         tm.begin();
      }
      testee.registerPendingPut(KEY1);
      assertTrue(testee.isPutValid(KEY1));      
   }
   
   public void testRegisteredPutWithInterveningKeyRemoval() throws Exception {
      registeredPutWithInterveningRemovalTest(false, false);
   }
   
   public void testRegisteredPutWithInterveningKeyRemovalTransactional() throws Exception {
      registeredPutWithInterveningRemovalTest(true, false);
   }
   
   public void testRegisteredPutWithInterveningRegionRemoval() throws Exception {
      registeredPutWithInterveningRemovalTest(false, true);
   }
   
   public void testRegisteredPutWithInterveningRegionRemovalTransactional() throws Exception {
      registeredPutWithInterveningRemovalTest(true, true);
   }

   private void registeredPutWithInterveningRemovalTest(boolean transactional, boolean removeRegion) throws Exception
   {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
      if (transactional) {
         tm.begin();
      }
      testee.registerPendingPut(KEY1);
      if (removeRegion) {
         testee.regionRemoved();
      }
      else {
         testee.keyRemoved(KEY1);
      }
      assertFalse(testee.isPutValid(KEY1));      
   }
   
   public void testDelayedNakedPutAfterKeyRemoval() throws Exception {
      delayedNakedPutAfterRemovalTest(false, false);
   }
   
   public void testDelayedNakedPutAfterKeyRemovalTransactional() throws Exception {
      delayedNakedPutAfterRemovalTest(true, false);
   }
   
   public void testDelayedNakedPutAfterRegionRemoval() throws Exception {
      delayedNakedPutAfterRemovalTest(false, true);
   }
   
   public void testDelayedNakedPutAfterRegionRemovalTransactional() throws Exception {
      delayedNakedPutAfterRemovalTest(true, true);
   }

   private void delayedNakedPutAfterRemovalTest(boolean transactional, boolean removeRegion) throws Exception
   {
      PutFromLoadValidator testee = new TestValidator(transactional ? tm : null, 100, 1000, 500, 10000);
      if (removeRegion) {
         testee.regionRemoved();
      }
      else {
         testee.keyRemoved(KEY1);
      }
      if (transactional) {
         tm.begin();
      }
      Thread.sleep(110);
      assertTrue(testee.isPutValid(KEY1));
      
   }
   
   private static class TestValidator extends PutFromLoadValidator {

      protected TestValidator(TransactionManager transactionManager, long nakedPutInvalidationPeriod,
            long pendingPutOveragePeriod, long pendingPutRecentPeriod, long maxPendingPutDelay)
      {
         super(transactionManager, nakedPutInvalidationPeriod, pendingPutOveragePeriod, pendingPutRecentPeriod,
               maxPendingPutDelay);
      }
      
   }
}
