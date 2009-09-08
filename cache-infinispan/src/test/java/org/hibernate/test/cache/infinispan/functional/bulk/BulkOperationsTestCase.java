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
package org.hibernate.test.cache.infinispan.functional.bulk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.test.cache.infinispan.functional.Contact;
import org.hibernate.test.cache.infinispan.functional.Customer;
import org.hibernate.transaction.CMTTransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BulkOperationsTestCase.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BulkOperationsTestCase extends FunctionalTestCase {

   private static final Logger log = LoggerFactory.getLogger(BulkOperationsTestCase.class);
   
   private TransactionManager tm;
            
   public BulkOperationsTestCase(String string) {
      super(string);
   }

   public String[] getMappings() {
      return new String[] { "cache/infinispan/functional/Contact.hbm.xml", "cache/infinispan/functional/Customer.hbm.xml" };
   }
   
   @Override
   public String getCacheConcurrencyStrategy() {
      return "transactional";
   }
   
   protected Class getTransactionFactoryClass() {
       return CMTTransactionFactory.class;
   }

   protected Class getConnectionProviderClass() {
      return org.hibernate.test.cache.infinispan.tm.XaConnectionProvider.class;
   }
  
   protected Class<? extends TransactionManagerLookup> getTransactionManagerLookupClass() {
      return org.hibernate.test.cache.infinispan.tm.XaTransactionManagerLookup.class;
   }

   public void configure(Configuration cfg) {
      super.configure(cfg);

      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
      cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
      cfg.setProperty(Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName());
      cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, getTransactionManagerLookupClass().getName());
      
      Class transactionFactory = getTransactionFactoryClass();
      cfg.setProperty( Environment.TRANSACTION_STRATEGY, transactionFactory.getName());
   }

   public void testBulkOperations() throws Throwable {
      System.out.println("*** testBulkOperations()");
      boolean cleanedUp = false;
      try {
         tm = getTransactionManagerLookupClass().newInstance().getTransactionManager(null);
         
         createContacts();

         List<Integer> rhContacts = getContactsByCustomer("Red Hat");
         assertNotNull("Red Hat contacts exist", rhContacts);
         assertEquals("Created expected number of Red Hat contacts", 10, rhContacts.size());

         assertEquals("Deleted all Red Hat contacts", 10, deleteContacts());

         List<Integer> jbContacts = getContactsByCustomer("JBoss");
         assertNotNull("JBoss contacts exist", jbContacts);
         assertEquals("JBoss contacts remain", 10, jbContacts.size());

         for (Integer id : rhContacts) {
            assertNull("Red Hat contact " + id + " cannot be retrieved", getContact(id));
         }
         rhContacts = getContactsByCustomer("Red Hat");
         if (rhContacts != null) {
            assertEquals("No Red Hat contacts remain", 0, rhContacts.size());
         }

         updateContacts("Kabir", "Updated");
         for (Integer id : jbContacts) {
            Contact contact = getContact(id);
            assertNotNull("JBoss contact " + id + " exists", contact);
            String expected = ("Kabir".equals(contact.getName())) ? "Updated" : "2222";
            assertEquals("JBoss contact " + id + " has correct TLF", expected, contact.getTlf());
         }

         List<Integer> updated = getContactsByTLF("Updated");
         assertNotNull("Got updated contacts", updated);
         assertEquals("Updated contacts", 5, updated.size());
      } catch(Throwable t) {
         cleanedUp = true;
         log.debug("Exceptional cleanup");
         cleanup(true);
         throw t;
      } finally {
         // cleanup the db so we can run this test multiple times w/o restarting the cluster
         if (!cleanedUp) {
            log.debug("Non exceptional cleanup");
            cleanup(false);
         }
      }
   }

   public void createContacts() throws Exception {
      log.debug("Create 10 contacts");
      tm.begin();
      try {
         for (int i = 0; i < 10; i++)
            createCustomer(i);
         tm.commit();
      } catch (Exception e) {
         log.error("Unable to create customer", e);
         tm.rollback();
         throw e;
      }
   }

   public int deleteContacts() throws Exception {
      String deleteHQL = "delete Contact where customer in ";
      deleteHQL += " (select customer FROM Customer as customer ";
      deleteHQL += " where customer.name = :cName)";

      tm.begin();
      try {

         Session session = getSessions().getCurrentSession();
         int rowsAffected = session.createQuery(deleteHQL).setFlushMode(FlushMode.AUTO)
                  .setParameter("cName", "Red Hat").executeUpdate();
         tm.commit();
         return rowsAffected;
      } catch (Exception e) {
         try {
            tm.rollback();
         } catch (Exception ee) {
            // ignored
         }
         throw e;
      }
   }

   public List<Integer> getContactsByCustomer(String customerName) throws Exception {
      String selectHQL = "select contact.id from Contact contact";
      selectHQL += " where contact.customer.name = :cName";

      log.debug("Get contacts for customer " + customerName);
      tm.begin();
      try {

         Session session = getSessions().getCurrentSession();
         List results = session.createQuery(selectHQL).setFlushMode(FlushMode.AUTO).setParameter("cName", customerName)
                  .list();
         tm.commit();
         return results;
      } catch (Exception e) {
         tm.rollback();
         throw e;
      }
   }

   public List<Integer> getContactsByTLF(String tlf) throws Exception {
      String selectHQL = "select contact.id from Contact contact";
      selectHQL += " where contact.tlf = :cTLF";

      tm.begin();
      try {

         Session session = getSessions().getCurrentSession();
         List results = session.createQuery(selectHQL).setFlushMode(FlushMode.AUTO).setParameter("cTLF", tlf).list();
         tm.commit();
         return results;
      } catch (Exception e) {
         tm.rollback();
         throw e;
      }
   }

   public int updateContacts(String name, String newTLF) throws Exception {
      String updateHQL = "update Contact set tlf = :cNewTLF where name = :cName";

      tm.begin();
      try {

         Session session = getSessions().getCurrentSession();
         int rowsAffected = session.createQuery(updateHQL).setFlushMode(FlushMode.AUTO).setParameter("cNewTLF", newTLF)
                  .setParameter("cName", name).executeUpdate();
         tm.commit();
         return rowsAffected;
      } catch (Exception e) {
         tm.rollback();
         throw e;
      }
   }

   public Contact getContact(Integer id) throws Exception {
      tm.begin();
      try {

         Session session = getSessions().getCurrentSession();
         Contact contact = (Contact) session.get(Contact.class, id);
         tm.commit();
         return contact;
      } catch (Exception e) {
         tm.rollback();
         throw e;
      }
   }

   public void cleanup(boolean ignore) throws Exception {
      String deleteContactHQL = "delete from Contact";
      String deleteCustomerHQL = "delete from Customer";
      tm.begin();
      try {
         Session session = getSessions().getCurrentSession();
         session.createQuery(deleteContactHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
         session.createQuery(deleteCustomerHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
         tm.commit();
      } catch (Exception e) {
         if (!ignore) {
            try {
               tm.rollback();
            } catch (Exception ee) {
               // ignored
            }
            throw e;
         }
      }
   }

   private Customer createCustomer(int id) throws Exception {
      System.out.println("CREATE CUSTOMER " + id);
      try {
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

         Session s = openSession();
         s.getTransaction().begin();
         s.persist(customer);
         s.getTransaction().commit();
         s.close();
         
         return customer;
      } finally {
         System.out.println("CREATE CUSTOMER " + id + " -  END");
      }
   }

}
