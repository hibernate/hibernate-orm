/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * BasicJdbcTransactionalTestCase.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicJdbcTransactionalTestCase extends SingleNodeTestCase {
   private static final Log log = LogFactory.getLog(BasicJdbcTransactionalTestCase.class);

   public BasicJdbcTransactionalTestCase(String string) {
      super(string);
   }

   protected Class<? extends TransactionFactory> getTransactionFactoryClass() {
      return JDBCTransactionFactory.class;
   }

   protected Class<? extends TransactionManagerLookup> getTransactionManagerLookupClass() {
      return null;
   }

   public void testCollectionCache() throws Exception {
      Item item = new Item("chris", "Chris's Item");
      Item another = new Item("another", "Owned Item");
      item.addItem(another);

      Session s = null;
      try {
         s = openSession();
         s.beginTransaction();
         s.persist(item);
         s.persist(another);
         s.getTransaction().commit();
      } catch (Exception e) {
         log.error("Exception", e);
         s.getTransaction().rollback();
         throw e;
      } finally {
         s.close();
      }

      try {
         s = openSession();
         s.beginTransaction();
         Item loaded = (Item) s.load(Item.class, item.getId());
         assertEquals(1, loaded.getItems().size());
      } catch (Exception e) {
         log.error("Exception", e);
         s.getTransaction().rollback();
         throw e;
      } finally {
         s.close();
      }

      try {
         s = openSession();
         s.beginTransaction();
         Statistics stats = s.getSessionFactory().getStatistics();
         SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
         Item loadedWithCachedCollection = (Item) s.load(Item.class, item.getId());
         stats.logSummary();
         assertEquals(item.getName(), loadedWithCachedCollection.getName());
         assertEquals(item.getItems().size(), loadedWithCachedCollection.getItems().size());
         assertEquals(1, cStats.getHitCount());
         Map cacheEntries = cStats.getEntries();
         assertEquals(1, cacheEntries.size());
      } catch (Exception e) {
         log.error("Exception", e);
         s.getTransaction().rollback();
         throw e;
      } finally {
         s.close();
      }
   }
}
