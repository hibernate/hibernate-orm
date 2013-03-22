/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.cache.infinispan.functional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.Test;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeConnectionProviderImpl;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaPlatformImpl;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author nikita_tovstoles@mba.berkeley.edu
 * @author Galder Zamarreño
 */
public class ConcurrentWriteTest extends SingleNodeTestCase {
	private static final Log log = LogFactory.getLog( ConcurrentWriteTest.class );
	private static final boolean trace = log.isTraceEnabled();
	/**
	 * when USER_COUNT==1, tests pass, when >4 tests fail
	 */
	private static final int USER_COUNT = 5;
	private static final int ITERATION_COUNT = 150;
	private static final int THINK_TIME_MILLIS = 10;
	private static final long LAUNCH_INTERVAL_MILLIS = 10;
	private static final Random random = new Random();

	/**
	 * kill switch used to stop all users when one fails
	 */
	private static volatile boolean TERMINATE_ALL_USERS = false;

	/**
	 * collection of IDs of all customers participating in this test
	 */
	private Set<Integer> customerIDs = new HashSet<Integer>();

	private TransactionManager tm;

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( DualNodeTestCase.NODE_ID_PROP, DualNodeTestCase.LOCAL );
		cfg.setProperty( DualNodeTestCase.NODE_ID_FIELD, DualNodeTestCase.LOCAL );
	}

	@Override
	protected boolean getUseQueryCache() {
		return true;
	}

	@Override
	protected TransactionManager getTransactionManager() {
		return DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.LOCAL );
	}

	@Override
	protected Class<? extends ConnectionProvider> getConnectionProviderClass() {
		return DualNodeConnectionProviderImpl.class;
	}

	@Override
	protected Class<? extends JtaPlatform> getJtaPlatform() {
		return DualNodeJtaPlatformImpl.class;
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		TERMINATE_ALL_USERS = false;
	}

	@Override
	protected void cleanupTest() throws Exception {
		try {
			super.cleanupTest();
		}
		finally {
			cleanup();
			// DualNodeJtaTransactionManagerImpl.cleanupTransactions();
			// DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
		}
	}

	@Test
	public void testPingDb() throws Exception {
		try {
			beginTx();
			sessionFactory()
					.getCurrentSession()
					.createQuery( "from " + Customer.class.getName() )
					.list();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
//         setRollbackOnly();
//         fail("failed to query DB; exception=" + e);
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testSingleUser() throws Exception {
		// setup
sessionFactory().getStatistics().clear();
		Customer customer = createCustomer( 0 );
		final Integer customerId = customer.getId();
		getCustomerIDs().add( customerId );

		assertNull( "contact exists despite not being added", getFirstContact( customerId ) );

		// check that cache was hit
		SecondLevelCacheStatistics customerSlcs = sessionFactory()
				.getStatistics()
				.getSecondLevelCacheStatistics( Customer.class.getName() );
		assertEquals( customerSlcs.getPutCount(), 1 );
		assertEquals( customerSlcs.getElementCountInMemory(), 1 );
		assertEquals( customerSlcs.getEntries().size(), 1 );

		log.infof( "Add contact to customer {0}", customerId );
		SecondLevelCacheStatistics contactsCollectionSlcs = sessionFactory()
				.getStatistics()
				.getSecondLevelCacheStatistics( Customer.class.getName() + ".contacts" );
		assertEquals( 1, contactsCollectionSlcs.getPutCount() );
		assertEquals( 1, contactsCollectionSlcs.getElementCountInMemory() );
		assertEquals( 1, contactsCollectionSlcs.getEntries().size() );

		final Contact contact = addContact( customerId );
		assertNotNull( "contact returned by addContact is null", contact );
		assertEquals(
				"Customer.contacts cache was not invalidated after addContact", 0,
				contactsCollectionSlcs.getElementCountInMemory()
		);

		assertNotNull( "Contact missing after successful add call", getFirstContact( customerId ) );

		// read everyone's contacts
		readEveryonesFirstContact();

		removeContact( customerId );
		assertNull( "contact still exists after successful remove call", getFirstContact( customerId ) );

	}

	@Test
	public void testManyUsers() throws Throwable {
		try {
			// setup - create users
			for ( int i = 0; i < USER_COUNT; i++ ) {
				Customer customer = createCustomer( 0 );
				getCustomerIDs().add( customer.getId() );
			}
			assertEquals( "failed to create enough Customers", USER_COUNT, getCustomerIDs().size() );

			final ExecutorService executor = Executors.newFixedThreadPool( USER_COUNT );

			CyclicBarrier barrier = new CyclicBarrier( USER_COUNT + 1 );
			List<Future<Void>> futures = new ArrayList<Future<Void>>( USER_COUNT );
			for ( Integer customerId : getCustomerIDs() ) {
				Future<Void> future = executor.submit( new UserRunner( customerId, barrier ) );
				futures.add( future );
				Thread.sleep( LAUNCH_INTERVAL_MILLIS ); // rampup
			}
//         barrier.await(); // wait for all threads to be ready
			barrier.await( 2, TimeUnit.MINUTES ); // wait for all threads to finish
			log.info( "All threads finished, let's shutdown the executor and check whether any exceptions were reported" );
			for ( Future<Void> future : futures ) {
				future.get();
			}
			log.info( "All future gets checked" );
		}
		catch (Throwable t) {
			log.error( "Error running test", t );
			throw t;
		}
	}

	public void cleanup() throws Exception {
		getCustomerIDs().clear();
		String deleteContactHQL = "delete from Contact";
		String deleteCustomerHQL = "delete from Customer";
		beginTx();
		try {
			Session session = sessionFactory().getCurrentSession();
			session.createQuery( deleteContactHQL ).setFlushMode( FlushMode.AUTO ).executeUpdate();
			session.createQuery( deleteCustomerHQL ).setFlushMode( FlushMode.AUTO ).executeUpdate();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	private Customer createCustomer(int nameSuffix) throws Exception {
		Customer customer = null;
		beginTx();
		try {
			customer = new Customer();
			customer.setName( "customer_" + nameSuffix );
			customer.setContacts( new HashSet<Contact>() );
			sessionFactory().getCurrentSession().persist( customer );
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
		return customer;
	}

	/**
	 * read first contact of every Customer participating in this test. this forces concurrent cache
	 * writes of Customer.contacts Collection cache node
	 *
	 * @return who cares
	 * @throws java.lang.Exception
	 */
	private void readEveryonesFirstContact() throws Exception {
		beginTx();
		try {
			for ( Integer customerId : getCustomerIDs() ) {
				if ( TERMINATE_ALL_USERS ) {
					setRollbackOnlyTx();
					return;
				}
				Customer customer = (Customer) sessionFactory()
						.getCurrentSession()
						.load( Customer.class, customerId );
				Set<Contact> contacts = customer.getContacts();
				if ( !contacts.isEmpty() ) {
					contacts.iterator().next();
				}
			}
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

   /**
    * -load existing Customer -get customer's contacts; return 1st one
    * 
    * @param customerId
    * @return first Contact or null if customer has none
    */
   private Contact getFirstContact(Integer customerId) throws Exception {
      assert customerId != null;
      Contact firstContact = null;
      beginTx();
      try {
         final Customer customer = (Customer) sessionFactory()
				 .getCurrentSession()
				 .load(Customer.class, customerId);
         Set<Contact> contacts = customer.getContacts();
         firstContact = contacts.isEmpty() ? null : contacts.iterator().next();
         if (TERMINATE_ALL_USERS)
            setRollbackOnlyTx();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
      return firstContact;
   }

   /**
    * -load existing Customer -create a new Contact and add to customer's contacts
    *
    * @param customerId
    * @return added Contact
    */
   private Contact addContact(Integer customerId) throws Exception {
      assert customerId != null;
      Contact contact = null;
      beginTx();
      try {
         final Customer customer = (Customer) sessionFactory()
				 .getCurrentSession()
				 .load(Customer.class, customerId);
         contact = new Contact();
         contact.setName("contact name");
         contact.setTlf("wtf is tlf?");
         contact.setCustomer(customer);
         customer.getContacts().add(contact);
         // assuming contact is persisted via cascade from customer
         if (TERMINATE_ALL_USERS)
            setRollbackOnlyTx();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
      return contact;
   }

   /**
    * remove existing 'contact' from customer's list of contacts
    *
    * @param customerId
    * @throws IllegalStateException
    *            if customer does not own a contact
    */
   private void removeContact(Integer customerId) throws Exception {
      assert customerId != null;

		beginTx();
		try {
			Customer customer = (Customer) sessionFactory()
					.getCurrentSession()
					.load( Customer.class, customerId );
			Set<Contact> contacts = customer.getContacts();
			if ( contacts.size() != 1 ) {
				throw new IllegalStateException(
						"can't remove contact: customer id=" + customerId
								+ " expected exactly 1 contact, " + "actual count=" + contacts.size()
				);
			}

			Contact contact = contacts.iterator().next();
			contacts.remove( contact );
			contact.setCustomer( null );

			// explicitly delete Contact because hbm has no 'DELETE_ORPHAN' cascade?
			// getEnvironment().getSessionFactory().getCurrentSession().delete(contact); //appears to
			// not be needed

			// assuming contact is persisted via cascade from customer

			if ( TERMINATE_ALL_USERS ) {
				setRollbackOnlyTx();
			}
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	/**
	 * @return the customerIDs
	 */
	public Set<Integer> getCustomerIDs() {
		return customerIDs;
	}

	private String statusOfRunnersToString(Set<UserRunner> runners) {
		assert runners != null;

		StringBuilder sb = new StringBuilder(
				"TEST CONFIG [userCount=" + USER_COUNT
						+ ", iterationsPerUser=" + ITERATION_COUNT + ", thinkTimeMillis="
						+ THINK_TIME_MILLIS + "] " + " STATE of UserRunners: "
		);

		for ( UserRunner r : runners ) {
			sb.append( r.toString() + System.getProperty( "line.separator" ) );
		}
		return sb.toString();
	}

	class UserRunner implements Callable<Void> {
		private final CyclicBarrier barrier;
		final private Integer customerId;
		private int completedIterations = 0;
		private Throwable causeOfFailure;

		public UserRunner(Integer cId, CyclicBarrier barrier) {
			assert cId != null;
			this.customerId = cId;
			this.barrier = barrier;
		}

		private boolean contactExists() throws Exception {
			return getFirstContact( customerId ) != null;
		}

		public Void call() throws Exception {
			// name this thread for easier log tracing
			Thread.currentThread().setName( "UserRunnerThread-" + getCustomerId() );
			log.info( "Wait for all executions paths to be ready to perform calls" );
			try {
//            barrier.await();
				for ( int i = 0; i < ITERATION_COUNT && !TERMINATE_ALL_USERS; i++ ) {
					contactExists();
					if ( trace ) {
						log.trace( "Add contact for customer " + customerId );
					}
					addContact( customerId );
					if ( trace ) {
						log.trace( "Added contact" );
					}
					thinkRandomTime();
					contactExists();
					thinkRandomTime();
					if ( trace ) {
						log.trace( "Read all customers' first contact" );
					}
					// read everyone's contacts
					readEveryonesFirstContact();
					if ( trace ) {
						log.trace( "Read completed" );
					}
					thinkRandomTime();
					if ( trace ) {
						log.trace( "Remove contact of customer" + customerId );
					}
					removeContact( customerId );
					if ( trace ) {
						log.trace( "Removed contact" );
					}
					contactExists();
					thinkRandomTime();
					++completedIterations;
					if ( trace ) {
						log.tracef( "Iteration completed {0}", completedIterations );
					}
				}
			}
			catch (Throwable t) {
				TERMINATE_ALL_USERS = true;
				log.error( "Error", t );
				throw new Exception( t );
				// rollback current transaction if any
				// really should not happen since above methods all follow begin-commit-rollback pattern
				// try {
				// if
				// (DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).getTransaction()
				// != null) {
				// DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).rollback();
				// }
				// } catch (SystemException ex) {
				// throw new RuntimeException("failed to rollback tx", ex);
				// }
			}
			finally {
				log.info( "Wait for all execution paths to finish" );
				barrier.await();
			}
			return null;
		}

		public boolean isSuccess() {
			return ITERATION_COUNT == getCompletedIterations();
		}

		public int getCompletedIterations() {
			return completedIterations;
		}

		public Throwable getCauseOfFailure() {
			return causeOfFailure;
		}

		public Integer getCustomerId() {
			return customerId;
		}

		@Override
		public String toString() {
			return super.toString() + "[customerId=" + getCustomerId() + " iterationsCompleted="
					+ getCompletedIterations() + " completedAll=" + isSuccess() + " causeOfFailure="
					+ (this.causeOfFailure != null ? getStackTrace( causeOfFailure ) : "") + "] ";
		}
	}

	public static String getStackTrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw, true );
		throwable.printStackTrace( pw );
		return sw.getBuffer().toString();
	}

	/**
	 * sleep between 0 and THINK_TIME_MILLIS.
	 *
	 * @throws RuntimeException if sleep is interrupted or TERMINATE_ALL_USERS flag was set to true i n the
	 * meantime
	 */
	private void thinkRandomTime() {
		try {
			Thread.sleep( random.nextInt( THINK_TIME_MILLIS ) );
		}
		catch (InterruptedException ex) {
			throw new RuntimeException( "sleep interrupted", ex );
		}

		if ( TERMINATE_ALL_USERS ) {
			throw new RuntimeException( "told to terminate (because a UserRunner had failed)" );
		}
	}

}
