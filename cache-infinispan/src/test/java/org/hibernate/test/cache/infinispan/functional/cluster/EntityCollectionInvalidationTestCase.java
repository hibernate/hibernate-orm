/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.test.cache.infinispan.functional.Contact;
import org.hibernate.test.cache.infinispan.functional.Customer;
import org.infinispan.Cache;
import org.infinispan.manager.CacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.jboss.util.collection.ConcurrentSet;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * EntityCollectionInvalidationTestCase.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityCollectionInvalidationTestCase extends DualNodeTestCase {
   private static final Log log = LogFactory.getLog(EntityCollectionInvalidationTestCase.class);
   private static final long SLEEP_TIME = 50l;
   private static final Integer CUSTOMER_ID = new Integer(1);
   static int test = 0;

   public EntityCollectionInvalidationTestCase(String string) {
      super(string);
   }

   protected String getEntityCacheConfigName() {
      return "entity";
   }

   public void testAll() throws Exception {
      log.info("*** testAll()");

      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      CacheManager localManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.LOCAL);
      // Cache localCache = localManager.getCache("entity");
      Cache localCustomerCache = localManager.getCache(Customer.class.getName());
      Cache localContactCache = localManager.getCache(Contact.class.getName());
      Cache localCollectionCache = localManager.getCache(Customer.class.getName() + ".contacts");
      MyListener localListener = new MyListener("local");
      localCustomerCache.addListener(localListener);
      localContactCache.addListener(localListener);
      localCollectionCache.addListener(localListener);
      TransactionManager localTM = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestCase.LOCAL);

      // Bind a listener to the "remote" cache
      CacheManager remoteManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.REMOTE);
      Cache remoteCustomerCache = remoteManager.getCache(Customer.class.getName());
      Cache remoteContactCache = remoteManager.getCache(Contact.class.getName());
      Cache remoteCollectionCache = remoteManager.getCache(Customer.class.getName() + ".contacts");
      MyListener remoteListener = new MyListener("remote");
      remoteCustomerCache.addListener(remoteListener);
      remoteContactCache.addListener(remoteListener);
      remoteCollectionCache.addListener(remoteListener);
      TransactionManager remoteTM = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestCase.REMOTE);

      SessionFactory localFactory = getEnvironment().getSessionFactory();
      SessionFactory remoteFactory = getSecondNodeEnvironment().getSessionFactory();

      try {
         assertTrue(remoteListener.isEmpty());
         assertTrue(localListener.isEmpty());
         
         log.debug("Create node 0");
         IdContainer ids = createCustomer(localFactory, localTM);

         assertTrue(remoteListener.isEmpty());
         assertTrue(localListener.isEmpty());
         
         // Sleep a bit to let async commit propagate. Really just to
         // help keep the logs organized for debugging any issues
         sleep(SLEEP_TIME);

         log.debug("Find node 0");
         // This actually brings the collection into the cache
         getCustomer(ids.customerId, localFactory, localTM);

         sleep(SLEEP_TIME);

         // Now the collection is in the cache so, the 2nd "get"
         // should read everything from the cache
         log.debug("Find(2) node 0");
         localListener.clear();
         getCustomer(ids.customerId, localFactory, localTM);

         // Check the read came from the cache
         log.debug("Check cache 0");
         assertLoadedFromCache(localListener, ids.customerId, ids.contactIds);

         log.debug("Find node 1");
         // This actually brings the collection into the cache since invalidation is in use
         getCustomer(ids.customerId, remoteFactory, remoteTM);
         
         // Now the collection is in the cache so, the 2nd "get"
         // should read everything from the cache
         log.debug("Find(2) node 1");
         remoteListener.clear();
         getCustomer(ids.customerId, remoteFactory, remoteTM);

         // Check the read came from the cache
         log.debug("Check cache 1");
         assertLoadedFromCache(remoteListener, ids.customerId, ids.contactIds);

         // Modify customer in remote
         remoteListener.clear();
         ids = modifyCustomer(ids.customerId, remoteFactory, remoteTM);
         sleep(250);
         assertLoadedFromCache(remoteListener, ids.customerId, ids.contactIds);

         // After modification, local cache should have been invalidated and hence should be empty
         assertEquals(0, getValidKeyCount(localCollectionCache.keySet()));
         assertEquals(0, getValidKeyCount(localCustomerCache.keySet()));
      } catch (Exception e) {
         log.error("Error", e);
         throw e;
      } finally {
         // cleanup the db
         log.debug("Cleaning up");
         cleanup(localFactory, localTM);
      }
   }

   private IdContainer createCustomer(SessionFactory sessionFactory, TransactionManager tm)
            throws Exception {
      log.debug("CREATE CUSTOMER");

      tm.begin();

      try {
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
         Set contactIds = new HashSet();
         contactIds.add(kabir.getId());
         contactIds.add(bill.getId());
         ids.contactIds = contactIds;

         return ids;
      } catch (Exception e) {
         log.error("Caught exception creating customer", e);
         try {
            tm.rollback();
         } catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         throw e;
      } finally {
         log.debug("CREATE CUSTOMER -  END");
      }
   }

   private Customer getCustomer(Integer id, SessionFactory sessionFactory, TransactionManager tm) throws Exception {
      log.debug("Find customer with id=" + id);
      tm.begin();
      try {
         Session session = sessionFactory.getCurrentSession();
         Customer customer = doGetCustomer(id, session, tm);
         tm.commit();
         return customer;
      } catch (Exception e) {
         try {
            tm.rollback();
         } catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         throw e;
      } finally {
         log.debug("Find customer ended.");
      }
   }
   
   private Customer doGetCustomer(Integer id, Session session, TransactionManager tm) throws Exception {
      Customer customer = (Customer) session.get(Customer.class, id);
      // Access all the contacts
      for (Iterator it = customer.getContacts().iterator(); it.hasNext();) {
         ((Contact) it.next()).getName();
      }
      return customer;
   }
   
   private IdContainer modifyCustomer(Integer id, SessionFactory sessionFactory, TransactionManager tm) throws Exception {
      log.debug("Modify customer with id=" + id);
      tm.begin();
      try {
         Session session = sessionFactory.getCurrentSession();
         IdContainer ids = new IdContainer();
         Set contactIds = new HashSet();
         Customer customer = doGetCustomer(id, session, tm);
         customer.setName("NewJBoss");
         ids.customerId = customer.getId();
         Set<Contact> contacts = customer.getContacts();
         for (Contact c : contacts) {
            contactIds.add(c.getId());
         }
         Contact contact = contacts.iterator().next();
         contacts.remove(contact);
         contactIds.remove(contact.getId());
         ids.contactIds = contactIds;
         contact.setCustomer(null);
         
         session.save(customer);
         tm.commit();
         return ids;
      } catch (Exception e) {
         try {
            tm.rollback();
         } catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         throw e;
      } finally {
         log.debug("Find customer ended.");
      }
   }

   private void cleanup(SessionFactory sessionFactory, TransactionManager tm) throws Exception {
      tm.begin();
      try {
         Session session = sessionFactory.getCurrentSession();
         Customer c = (Customer) session.get(Customer.class, CUSTOMER_ID);
         if (c != null) {
            Set contacts = c.getContacts();
            for (Iterator it = contacts.iterator(); it.hasNext();)
               session.delete(it.next());
            c.setContacts(null);
            session.delete(c);
         }

         tm.commit();
      } catch (Exception e) {
         try {
            tm.rollback();
         } catch (Exception e1) {
            log.error("Exception rolling back txn", e1);
         }
         log.error("Caught exception in cleanup", e);
      }
   }

   private void assertLoadedFromCache(MyListener listener, Integer custId, Set contactIds) {
      assertTrue("Customer#" + custId + " was in cache", listener.visited.contains("Customer#"
               + custId));
      for (Iterator it = contactIds.iterator(); it.hasNext();) {
         Integer contactId = (Integer) it.next();
         assertTrue("Contact#" + contactId + " was in cache", listener.visited.contains("Contact#"
                  + contactId));
         assertTrue("Contact#" + contactId + " was in cache", listener.visited.contains("Contact#"
                  + contactId));
      }
      assertTrue("Customer.contacts" + custId + " was in cache", listener.visited
               .contains("Customer.contacts#" + custId));
   }

   protected int getValidKeyCount(Set keys) {
      int result = 0;
      for (Object key : keys) {
         if (!(CacheHelper.isEvictAllNotification(key))) {
            result++;
         }
      }
      return result;
  }

   @Listener
   public static class MyListener {
      private static final Log log = LogFactory.getLog(MyListener.class);
      private Set<String> visited = new ConcurrentSet<String>();
      private final String name;
      
      public MyListener(String name) {
         this.name = name;
      }      

      public void clear() {
         visited.clear();
      }
      
      public boolean isEmpty() {
         return visited.isEmpty();
      }

      @CacheEntryVisited
      public void nodeVisited(CacheEntryVisitedEvent event) {
         log.debug(event.toString());
         if (!event.isPre()) {
            CacheKey cacheKey = (CacheKey) event.getKey();
            Integer primKey = (Integer) cacheKey.getKey();
            String key = (String) cacheKey.getEntityOrRoleName() + '#' + primKey;
            log.debug("MyListener[" + name +"] - Visiting key " + key);
            // String name = fqn.toString();
            String token = ".functional.";
            int index = key.indexOf(token);
            if (index > -1) {
               index += token.length();
               key = key.substring(index);
               log.debug("MyListener[" + name +"] - recording visit to " + key);
               visited.add(key);
            }
         }
      }
   }

   private class IdContainer {
      Integer customerId;
      Set<Integer> contactIds;
   }

}
