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
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;
import org.junit.Test;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.test.cache.infinispan.functional.Contact;
import org.hibernate.test.cache.infinispan.functional.Customer;
import org.hibernate.test.cache.infinispan.tm.JtaPlatformImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * BulkOperationsTestCase.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BulkOperationsTestCase extends BaseCoreFunctionalTestCase {
	private TransactionManager tm;

	@Override
	public String[] getMappings() {
		return new String[] {
				"cache/infinispan/functional/Contact.hbm.xml",
				"cache/infinispan/functional/Customer.hbm.xml"
		};
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	protected Class<? extends RegionFactory> getCacheRegionFactory() {
		return SingleNodeTestCase.TestInfinispanRegionFactory.class;
	}

	protected Class<? extends TransactionFactory> getTransactionFactoryClass() {
		return CMTTransactionFactory.class;
	}

	protected Class<? extends ConnectionProvider> getConnectionProviderClass() {
		return org.hibernate.test.cache.infinispan.tm.XaConnectionProvider.class;
	}

	protected JtaPlatform getJtaPlatform() {
		return new JtaPlatformImpl();
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "false" );
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName() );
		cfg.getProperties().put( AvailableSettings.JTA_PLATFORM, getJtaPlatform() );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
	}

	@Test
	public void testBulkOperations() throws Throwable {
		boolean cleanedUp = false;
		try {
			tm = getJtaPlatform().retrieveTransactionManager();

			createContacts();

			List<Integer> rhContacts = getContactsByCustomer( "Red Hat" );
			assertNotNull( "Red Hat contacts exist", rhContacts );
			assertEquals( "Created expected number of Red Hat contacts", 10, rhContacts.size() );

			SecondLevelCacheStatistics contactSlcs = sessionFactory()
					.getStatistics()
					.getSecondLevelCacheStatistics( Contact.class.getName() );
			assertEquals( 20, contactSlcs.getElementCountInMemory() );

			assertEquals( "Deleted all Red Hat contacts", 10, deleteContacts() );
			assertEquals( 0, contactSlcs.getElementCountInMemory() );

			List<Integer> jbContacts = getContactsByCustomer( "JBoss" );
			assertNotNull( "JBoss contacts exist", jbContacts );
			assertEquals( "JBoss contacts remain", 10, jbContacts.size() );

			for ( Integer id : rhContacts ) {
				assertNull( "Red Hat contact " + id + " cannot be retrieved", getContact( id ) );
			}
			rhContacts = getContactsByCustomer( "Red Hat" );
			if ( rhContacts != null ) {
				assertEquals( "No Red Hat contacts remain", 0, rhContacts.size() );
			}

			updateContacts( "Kabir", "Updated" );
			assertEquals( 0, contactSlcs.getElementCountInMemory() );
			for ( Integer id : jbContacts ) {
				Contact contact = getContact( id );
				assertNotNull( "JBoss contact " + id + " exists", contact );
				String expected = ("Kabir".equals( contact.getName() )) ? "Updated" : "2222";
				assertEquals( "JBoss contact " + id + " has correct TLF", expected, contact.getTlf() );
			}

			List<Integer> updated = getContactsByTLF( "Updated" );
			assertNotNull( "Got updated contacts", updated );
			assertEquals( "Updated contacts", 5, updated.size() );

			updateContactsWithOneManual( "Kabir", "UpdatedAgain" );
			assertEquals( contactSlcs.getElementCountInMemory(), 0 );
			for ( Integer id : jbContacts ) {
				Contact contact = getContact( id );
				assertNotNull( "JBoss contact " + id + " exists", contact );
				String expected = ("Kabir".equals( contact.getName() )) ? "UpdatedAgain" : "2222";
				assertEquals( "JBoss contact " + id + " has correct TLF", expected, contact.getTlf() );
			}

			updated = getContactsByTLF( "UpdatedAgain" );
			assertNotNull( "Got updated contacts", updated );
			assertEquals( "Updated contacts", 5, updated.size() );
		}
		catch (Throwable t) {
			cleanedUp = true;
			cleanup( true );
			throw t;
		}
		finally {
			// cleanup the db so we can run this test multiple times w/o restarting the cluster
			if ( !cleanedUp ) {
				cleanup( false );
			}
		}
	}

	public void createContacts() throws Exception {
		tm.begin();
		try {
			for ( int i = 0; i < 10; i++ ) {
				createCustomer( i );
			}
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	public int deleteContacts() throws Exception {
		String deleteHQL = "delete Contact where customer in ";
		deleteHQL += " (select customer FROM Customer as customer ";
		deleteHQL += " where customer.name = :cName)";

		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			int rowsAffected = session.createQuery( deleteHQL ).setFlushMode( FlushMode.AUTO )
					.setParameter( "cName", "Red Hat" ).executeUpdate();
			tm.commit();
			return rowsAffected;
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				try {
					tm.rollback();
				}
				catch (Exception ee) {
					// ignored
				}
			}
		}
	}

	@SuppressWarnings( {"unchecked"})
	public List<Integer> getContactsByCustomer(String customerName) throws Exception {
		String selectHQL = "select contact.id from Contact contact";
		selectHQL += " where contact.customer.name = :cName";

		tm.begin();
		try {

			Session session = sessionFactory().getCurrentSession();
			return session.createQuery( selectHQL )
					.setFlushMode( FlushMode.AUTO )
					.setParameter( "cName", customerName )
					.list();
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	@SuppressWarnings( {"unchecked"})
	public List<Integer> getContactsByTLF(String tlf) throws Exception {
		String selectHQL = "select contact.id from Contact contact";
		selectHQL += " where contact.tlf = :cTLF";

		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			return session.createQuery( selectHQL )
					.setFlushMode( FlushMode.AUTO )
					.setParameter( "cTLF", tlf )
					.list();
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	public int updateContacts(String name, String newTLF) throws Exception {
		String updateHQL = "update Contact set tlf = :cNewTLF where name = :cName";
		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			return session.createQuery( updateHQL )
					.setFlushMode( FlushMode.AUTO )
					.setParameter( "cNewTLF", newTLF )
					.setParameter( "cName", name )
					.executeUpdate();
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	public int updateContactsWithOneManual(String name, String newTLF) throws Exception {
		String queryHQL = "from Contact c where c.name = :cName";
		String updateHQL = "update Contact set tlf = :cNewTLF where name = :cName";
		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			@SuppressWarnings("unchecked")
			List<Contact> list = session.createQuery( queryHQL ).setParameter( "cName", name ).list();
			list.get( 0 ).setTlf( newTLF );
			return session.createQuery( updateHQL )
					.setFlushMode( FlushMode.AUTO )
					.setParameter( "cNewTLF", newTLF )
					.setParameter( "cName", name )
					.executeUpdate();
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	public Contact getContact(Integer id) throws Exception {
		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			return (Contact) session.get( Contact.class, id );
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				tm.rollback();
			}
		}
	}

	public void cleanup(boolean ignore) throws Exception {
		String deleteContactHQL = "delete from Contact";
		String deleteCustomerHQL = "delete from Customer";
		tm.begin();
		try {
			Session session = sessionFactory().getCurrentSession();
			session.createQuery( deleteContactHQL ).setFlushMode( FlushMode.AUTO ).executeUpdate();
			session.createQuery( deleteCustomerHQL ).setFlushMode( FlushMode.AUTO ).executeUpdate();
		}
		catch (Exception e) {
			tm.setRollbackOnly();
			throw e;
		}
		finally {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.commit();
			}
			else {
				if ( !ignore ) {
					try {
						tm.rollback();
					}
					catch (Exception ee) {
						// ignored
					}
				}
			}
		}
	}

	private Customer createCustomer(int id) throws Exception {
		System.out.println( "CREATE CUSTOMER " + id );
		try {
			Customer customer = new Customer();
			customer.setName( (id % 2 == 0) ? "JBoss" : "Red Hat" );
			Set<Contact> contacts = new HashSet<Contact>();

			Contact kabir = new Contact();
			kabir.setCustomer( customer );
			kabir.setName( "Kabir" );
			kabir.setTlf( "1111" );
			contacts.add( kabir );

			Contact bill = new Contact();
			bill.setCustomer( customer );
			bill.setName( "Bill" );
			bill.setTlf( "2222" );
			contacts.add( bill );

			customer.setContacts( contacts );

			Session s = openSession();
			s.getTransaction().begin();
			s.persist( customer );
			s.getTransaction().commit();
			s.close();

			return customer;
		}
		finally {
			System.out.println( "CREATE CUSTOMER " + id + " -  END" );
		}
	}

}
