/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.test.cache.jbc.functional.bulk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.test.cache.jbc.functional.CacheTestCaseBase;
import org.hibernate.test.cache.jbc.functional.Contact;
import org.hibernate.test.cache.jbc.functional.Customer;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;
import org.hibernate.transaction.CMTTransactionFactory;

/**
 * Sample client for the jboss container.
 *
 * @author Brian Stansberry
 * @version $Id: EntityUnitTestCase.java 60697 2007-02-20 05:08:31Z bstansberry@jboss.com $
 */
public class PessimisticBulkOperationsTest
extends CacheTestCaseBase
{
   public PessimisticBulkOperationsTest(String name)
   {
      super(name);
   } 

   @Override
   protected Class getCacheRegionFactory()
   {
      return MultiplexedJBossCacheRegionFactory.class;
   }

   @Override
   protected void configureCacheFactory(Configuration cfg)
   {
      cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, 
                      getEntityCacheConfigName());
   }

   protected String getEntityCacheConfigName() {
       return "pessimistic-entity";
   }

   @Override
   protected boolean getUseQueryCache()
   {
      return false;
   }

   @Override
   protected Class getTransactionFactoryClass() {
       return CMTTransactionFactory.class;
   }
   
   public void testBulkOperations() throws Exception
   {
      System.out.println("*** testBulkOperations()");
      try
      {
         createContacts();
         
         List<Integer> rhContacts = getContactsByCustomer("Red Hat");
         assertNotNull("Red Hat contacts exist", rhContacts);
         assertEquals("Created expected number of Red Hat contacts", 10, rhContacts.size());
         
         SecondLevelCacheStatistics contactSlcs = getEnvironment().getSessionFactory().getStatistics().getSecondLevelCacheStatistics(
               getPrefixedRegionName(Contact.class.getName()));
         assertEquals(contactSlcs.getElementCountInMemory(), 20);
         
         assertEquals("Deleted all Red Hat contacts", 10, deleteContacts());
         assertEquals(0, contactSlcs.getElementCountInMemory());
         
         List<Integer> jbContacts = getContactsByCustomer("JBoss");
         assertNotNull("JBoss contacts exist", jbContacts);
         assertEquals("JBoss contacts remain", 10, jbContacts.size());
         
         for (Integer id : rhContacts)
         {
            assertNull("Red Hat contact " + id + " cannot be retrieved",
                       getContact(id));
         }
         rhContacts = getContactsByCustomer("Red Hat");
         if (rhContacts != null)
         {
            assertEquals("No Red Hat contacts remain", 0, rhContacts.size());
         }
         
         updateContacts("Kabir", "Updated");
         assertEquals(contactSlcs.getElementCountInMemory(), 0);
         for (Integer id : jbContacts)
         {
            Contact contact = getContact(id);
            assertNotNull("JBoss contact " + id + " exists", contact);
            String expected = ("Kabir".equals(contact.getName())) ? "Updated" : "2222";
            assertEquals("JBoss contact " + id + " has correct TLF",
                         expected, contact.getTlf());
         }
         
         List<Integer> updated = getContactsByTLF("Updated");
         assertNotNull("Got updated contacts", updated);
         assertEquals("Updated contacts", 5, updated.size());
         
         updateContactsWithOneManual("Kabir", "UpdatedAgain");
         assertEquals(contactSlcs.getElementCountInMemory(), 0);
         for (Integer id : jbContacts)
         {
            Contact contact = getContact(id);
            assertNotNull("JBoss contact " + id + " exists", contact);
            String expected = ("Kabir".equals(contact.getName())) ? "UpdatedAgain" : "2222";
            assertEquals("JBoss contact " + id + " has correct TLF",
                         expected, contact.getTlf());
         }
         
         updated = getContactsByTLF("UpdatedAgain");
         assertNotNull("Got updated contacts", updated);
         assertEquals("Updated contacts", 5, updated.size());
      }
      finally
      {
         // cleanup the db so we can run this test multiple times w/o restarting the cluster
         cleanup();
      }
   }
   
   public void createContacts() throws Exception
   {
      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          for (int i = 0; i < 10; i++)
              createCustomer(i);
          SimpleJtaTransactionManagerImpl.getInstance().commit();
      }
      catch (Exception e) {
         SimpleJtaTransactionManagerImpl.getInstance().rollback();
         throw e;
      }
   }

   public int deleteContacts() throws Exception
   {
      String deleteHQL = "delete Contact where customer in ";
      deleteHQL += " (select customer FROM Customer as customer ";
      deleteHQL += " where customer.name = :cName)";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          int rowsAffected = session.createQuery(deleteHQL)
                                .setFlushMode(FlushMode.AUTO)
                                .setParameter("cName", "Red Hat")
                                .executeUpdate();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return rowsAffected;
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }
   }
   
   public List<Integer> getContactsByCustomer(String customerName)
       throws Exception
   {
      String selectHQL = "select contact.id from Contact contact";
      selectHQL += " where contact.customer.name = :cName";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          List results = session.createQuery(selectHQL)
                            .setFlushMode(FlushMode.AUTO)
                            .setParameter("cName", customerName)
                            .list();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return results;  
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }    
   }
   
   public List<Integer> getContactsByTLF(String tlf) throws Exception
   {
      String selectHQL = "select contact.id from Contact contact";
      selectHQL += " where contact.tlf = :cTLF";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          List results = session.createQuery(selectHQL)
                            .setFlushMode(FlushMode.AUTO)
                            .setParameter("cTLF", tlf)
                            .list();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return results;       
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }     
   }

   public int updateContacts(String name, String newTLF) throws Exception
   {
      String updateHQL = "update Contact set tlf = :cNewTLF where name = :cName";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          int rowsAffected = session.createQuery(updateHQL)
                                .setFlushMode(FlushMode.AUTO)
                                .setParameter("cNewTLF", newTLF)
                                .setParameter("cName", name)
                                .executeUpdate();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return rowsAffected;       
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }     
   }

   public int updateContactsWithOneManual(String name, String newTLF) throws Exception
   {
      String queryHQL = "from Contact c where c.name = :cName";
      String updateHQL = "update Contact set tlf = :cNewTLF where name = :cName";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          
          @SuppressWarnings("unchecked")
          List<Contact> list = session.createQuery(queryHQL).setParameter("cName", name).list();
          list.get(0).setTlf(newTLF);
          
          int rowsAffected = session.createQuery(updateHQL)
                                .setFlushMode(FlushMode.AUTO)
                                .setParameter("cNewTLF", newTLF)
                                .setParameter("cName", name)
                                .executeUpdate();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return rowsAffected;       
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }     
   }
   
   public Contact getContact(Integer id) throws Exception
   {

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          Contact contact = (Contact) session.get(Contact.class, id);
          SimpleJtaTransactionManagerImpl.getInstance().commit();
          return contact;       
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }     
   }
   
   public void cleanup() throws Exception
   {
      String deleteContactHQL = "delete from Contact";
      String deleteCustomerHQL = "delete from Customer";

      SimpleJtaTransactionManagerImpl.getInstance().begin();
      try {
          
          Session session = getSessions().getCurrentSession();
          session.createQuery(deleteContactHQL)
                 .setFlushMode(FlushMode.AUTO)
                 .executeUpdate();
          session.createQuery(deleteCustomerHQL)
                 .setFlushMode(FlushMode.AUTO)
                 .executeUpdate();
          SimpleJtaTransactionManagerImpl.getInstance().commit();
      }
      catch (Exception e) {
          SimpleJtaTransactionManagerImpl.getInstance().rollback();
          throw e;         
      }
      
   }
   
   private Customer createCustomer(int id) throws Exception
   {
      System.out.println("CREATE CUSTOMER " + id);
      try
      {
         Customer customer = new Customer();
         customer.setName((id % 2 == 0) ? "JBoss" : "Red Hat");
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

         getSessions().getCurrentSession().persist(customer);
         return customer;
      }
      finally
      {
         System.out.println("CREATE CUSTOMER " +  id + " -  END");         
      }
   }
}
