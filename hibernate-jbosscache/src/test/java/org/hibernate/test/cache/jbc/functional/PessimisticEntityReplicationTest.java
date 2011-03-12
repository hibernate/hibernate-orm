/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.cache.jbc.functional;

import java.util.HashSet;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc.functional.util.DualNodeTestUtil;
import org.hibernate.test.cache.jbc.functional.util.TestCacheInstanceManager;
import org.hibernate.test.cache.jbc.functional.util.TestJBossCacheRegionFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheManager;
import org.jboss.cache.Fqn;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeVisited;
import org.jboss.cache.notifications.event.NodeVisitedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the earlier JBoss EJB3 project's 
 * org.jboss.ejb3.test.clusteredentity.unit.EntityUnitTestCase
 *
 */
public class PessimisticEntityReplicationTest
extends DualNodeTestCaseBase
{
   protected final Logger log = LoggerFactory.getLogger(getClass());

   private static final long SLEEP_TIME = 50l;
   
   private static final Integer CUSTOMER_ID = new Integer(1);
   
   static int test = 0;
   
   public PessimisticEntityReplicationTest(String name)
   {
      super(name);
   }

   @Override
   protected Class<?> getCacheRegionFactory()
   {
      return TestJBossCacheRegionFactory.class;
   }

   @Override
   protected boolean getUseQueryCache()
   {
      return false;
   }

   @Override
   protected void configureCacheFactory(Configuration cfg)
   {
      cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, 
                      getEntityCacheConfigName());      
   }

   protected String getEntityCacheConfigName() {
       return "pessimistic-shared";
   }
   

   public void testAll() throws Exception
   {
      System.out.println("*** testAll()");
      
      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      CacheManager localManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.LOCAL);
      Cache<Object, Object> localCache = localManager.getCache(getEntityCacheConfigName(), true);
      MyListener localListener = new MyListener();
      localCache.addCacheListener(localListener);
      
      TransactionManager localTM = localCache.getConfiguration().getRuntimeConfig().getTransactionManager();
      
      // Bind a listener to the "remote" cache
      CacheManager remoteManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.REMOTE);
      Cache<Object, Object> remoteCache = remoteManager.getCache(getEntityCacheConfigName(), true);
      MyListener remoteListener = new MyListener();
      remoteCache.addCacheListener(remoteListener);      
      
      TransactionManager remoteTM = remoteCache.getConfiguration().getRuntimeConfig().getTransactionManager();
      
      SessionFactory localFactory = getEnvironment().getSessionFactory();
      SessionFactory remoteFactory = getSecondNodeEnvironment().getSessionFactory();
      
      try
      {
         System.out.println("Create node 0");
         IdContainer ids = createCustomer(localFactory, localTM);
         
         // Sleep a bit to let async commit propagate. Really just to
         // help keep the logs organized for debugging any issues
         sleep(SLEEP_TIME);
         
         System.out.println("Find node 0");
         // This actually brings the collection into the cache
         getCustomer(ids.customerId, localFactory, localTM);
         
         sleep(SLEEP_TIME);
         
         // Now the collection is in the cache so, the 2nd "get"
         // should read everything from the cache
         System.out.println("Find(2) node 0");         
         localListener.clear();
         getCustomer(ids.customerId, localFactory, localTM);
         
         //Check the read came from the cache
         System.out.println("Check cache 0");
         assertLoadedFromCache(localListener, ids.customerId, ids.contactIds);
         
         System.out.println("Find node 1");
         getCustomer(ids.customerId, remoteFactory, remoteTM);
   
         //Check everything was in cache
         System.out.println("Check cache 1");
         assertLoadedFromCache(remoteListener, ids.customerId, ids.contactIds);
      }
      finally
      {
         // cleanup the db
         System.out.println("Cleaning up");
         cleanup(localFactory, localTM);
      }
   }
   
   private IdContainer createCustomer(SessionFactory sessionFactory, TransactionManager tm)
      throws Exception
   {
      System.out.println("CREATE CUSTOMER");
      
      tm.begin(); 

      try
      {
         Session session = sessionFactory.getCurrentSession();
         
         Customer customer = new Customer();
         customer.setName("JBoss");
         Set<Contact> contacts = new HashSet<Contact>();
         
         Contact kabir = new Contact();
         kabir.setCustomer(customer);
         kabir.setName("Kabir");
         kabir.setTlf("1111");
         contacts.add(kabir);
         
         Contact bill = new Contact();
         bill.setCustomer(customer);
         bill.setName("Bill");
         bill.setTlf("2222");
         contacts.add(bill);

         customer.setContacts(contacts);

         session.save(customer);
         tm.commit();
         
         IdContainer ids = new IdContainer();
         ids.customerId = customer.getId();
         Set<Integer> contactIds = new HashSet<Integer>();
         contactIds.add(kabir.getId());
         contactIds.add(bill.getId());
         ids.contactIds = contactIds;
         
         return ids;
      }
      catch (Exception e)
      {
         log.error("Caught exception creating customer", e);
         try {
            tm.rollback();
         }
         catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         throw e;
      }
      finally
      {
         System.out.println("CREATE CUSTOMER -  END");         
      }
   }

   private Customer getCustomer(Integer id, SessionFactory sessionFactory, TransactionManager tm)
       throws Exception
   {      
      System.out.println("FIND CUSTOMER");
      
      tm.begin();
      try
      {
         Session session = sessionFactory.getCurrentSession();
         Customer customer = (Customer) session.get(Customer.class, id);
         // Access all the contacts
         for (Contact contact : customer.getContacts()) {
            contact.getName();
         }
         tm.commit();
         return customer;
      }
      catch (Exception e)
      {
         try {
            tm.rollback();
         }
         catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         throw e;
      }
      finally
      {
         System.out.println("FIND CUSTOMER -  END");         
      }
   }
   
   private void cleanup(SessionFactory sessionFactory, TransactionManager tm) throws Exception
   {
      tm.begin();
      try
      {
         Session session = sessionFactory.getCurrentSession();
         Customer c = (Customer) session.get(Customer.class, CUSTOMER_ID);
         if (c != null)
         {
            for (Contact contact : c.getContacts())
               session.delete(contact);
            c.setContacts(null);
            session.delete(c);
         }
         
         tm.commit();
      }
      catch (Exception e)
      {
         try {
            tm.rollback();
         }
         catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         log.error("Caught exception in cleanup", e);
      }
   }
   
   private void assertLoadedFromCache(MyListener listener, Integer custId, Set<Integer> contactIds)
   {
      assertTrue("Customer#" + custId + " was in cache", listener.visited.contains("Customer#" + custId));
      for (Integer contactId : contactIds) {
          assertTrue("Contact#"+ contactId + " was in cache", listener.visited.contains("Contact#"+ contactId));
          assertTrue("Contact#"+ contactId + " was in cache", listener.visited.contains("Contact#"+ contactId));
      }
      assertTrue("Customer.contacts" + custId + " was in cache", 
                 listener.visited.contains("Customer.contacts#" + custId));      
   }
   
   @CacheListener
   public class MyListener
   {
      HashSet<String> visited = new HashSet<String>(); 
      
      public void clear()
      {
         visited.clear();
      }
      
      @NodeVisited
      public void nodeVisited(NodeVisitedEvent event)
      {
         System.out.println(event);
         
         if (!event.isPre())
         {
            @SuppressWarnings("unchecked")
            Fqn fqn = event.getFqn();
            System.out.println("MyListener - Visiting node " + fqn.toString());
            String name = fqn.toString();
            String token = ".functional.";
            int index = name.indexOf(token);
            if (index > -1)
            {
               index += token.length();
               name = name.substring(index);
               System.out.println("MyListener - recording visit to " + name);
               visited.add(name);
            }
         }
      }
   }
   
   private class IdContainer {
      Integer customerId;
      Set<Integer> contactIds;
   }
}
