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
package org.hibernate.cache.infinispan.util;

import java.util.concurrent.Callable;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

/**
 * Helper for dealing with Infinisan cache instances.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CacheHelper {

   /**
    * Disallow external instantiation of CacheHelper.
    */
   private CacheHelper() {
   }

   public static <T> T withinTx(TransactionManager tm, Callable<T> c) throws Exception {
      tm.begin();
      try {
         return c.call();
      } catch (Exception e) {
         tm.setRollbackOnly();
         throw e;
      } finally {
         if (tm.getStatus() == Status.STATUS_ACTIVE) tm.commit();
         else tm.rollback();
      }
   }

}
