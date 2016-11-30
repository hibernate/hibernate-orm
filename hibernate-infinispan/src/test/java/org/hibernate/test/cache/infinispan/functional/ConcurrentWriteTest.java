/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.stat.SecondLevelCacheStatistics;

import org.hibernate.test.cache.infinispan.functional.entities.Contact;
import org.hibernate.test.cache.infinispan.functional.entities.Customer;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author nikita_tovstoles@mba.berkeley.edu
 * @author Galder Zamarreño
 */
public class ConcurrentWriteTest extends SingleNodeTest {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( ConcurrentWriteTest.class );
	private static final boolean trace = log.isTraceEnabled();
	/**
	 * when USER_COUNT==1, tests pass, when >4 tests fail
	 */
	private static final int USER_COUNT = 5;
	private static final int ITERATION_COUNT = 150;
	private static final int THINK_TIME_MILLIS = 10;
	private static final long LAUNCH_INTERVAL_MILLIS = 10;
	private static final Random random = new Random();
	private static final TestTimeService TIME_SERVICE = new TestTimeService();

	/**
	 * kill switch used to stop all users when one fails
	 */
	private static volatile boolean TERMINATE_ALL_USERS = false;

	/**
	 * collection of IDs of all customers participating in this test
	 */
	private Set<Integer> customerIDs = new HashSet<Integer>();

	@Override
	public List<Object[]> getParameters() {
		return getParameters(true, true, false, true);
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		TERMINATE_ALL_USERS = false;
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings(settings);
		settings.put(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
	}

	@Override
	protected void cleanupTest() throws Exception {
		try {
			super.cleanupTest();
		}
		finally {
			cleanup();
		}
	}

	@Test
	public void testPingDb() throws Exception {
		withTxSession(s -> s.createQuery( "from " + Customer.class.getName() ).list());
	}

	@Test
	public void testSingleUser() throws Exception {
		// setup
		sessionFactory().getStatistics().clear();
		// wait a while to make sure that timestamp comparison works after invalidateRegion
		TIME_SERVICE.advance(1);

		Customer customer = createCustomer( 0 );
		final Integer customerId = customer.getId();
		getCustomerIDs().add( customerId );

		// wait a while to make sure that timestamp comparison works after collection remove (during insert)
		TIME_SERVICE.advance(1);

		assertNull( "contact exists despite not being added", getFirstContact( customerId ) );

		// check that cache was hit
		SecondLevelCacheStatistics customerSlcs = sessionFactory()
				.getStatistics()
				.getSecondLevelCacheStatistics( Customer.class.getName() );
		assertEquals( 1, customerSlcs.getPutCount() );
		assertEquals( 1, customerSlcs.getElementCountInMemory() );
		assertEquals( 1, customerSlcs.getEntries().size() );

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

	// Ignoring the test as it's more of a stress-test: this should be enabled manually
	@Ignore
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
			barrier.await( 2, TimeUnit.MINUTES ); // wait for all threads to finish
			log.info( "All threads finished, let's shutdown the executor and check whether any exceptions were reported" );
			for ( Future<Void> future : futures ) {
				future.get();
			}
			executor.shutdown();
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
		withTxSession(s -> {
			s.createQuery(deleteContactHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
			s.createQuery(deleteCustomerHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
		});
	}

	private Customer createCustomer(int nameSuffix) throws Exception {
		return withTxSessionApply(s -> {
			Customer customer = new Customer();
			customer.setName( "customer_" + nameSuffix );
			customer.setContacts( new HashSet<Contact>() );
			s.persist( customer );
			return customer;
		});
	}

	/**
	 * read first contact of every Customer participating in this test. this forces concurrent cache
	 * writes of Customer.contacts Collection cache node
	 *
	 * @return who cares
	 * @throws java.lang.Exception
	 */
	private void readEveryonesFirstContact() throws Exception {
		withTxSession(s -> {
			for ( Integer customerId : getCustomerIDs() ) {
				if ( TERMINATE_ALL_USERS ) {
					markRollbackOnly(s);
					return;
				}
				Customer customer = s.load( Customer.class, customerId );
				Set<Contact> contacts = customer.getContacts();
				if ( !contacts.isEmpty() ) {
					contacts.iterator().next();
				}
			}
		});
	}

	/**
	 * -load existing Customer -get customer's contacts; return 1st one
	 *
	 * @param customerId
	 * @return first Contact or null if customer has none
	 */
	private Contact getFirstContact(Integer customerId) throws Exception {
		assert customerId != null;
		return withTxSessionApply(s -> {
			Customer customer = s.load(Customer.class, customerId);
			Set<Contact> contacts = customer.getContacts();
			Contact firstContact = contacts.isEmpty() ? null : contacts.iterator().next();
			if (TERMINATE_ALL_USERS) {
				markRollbackOnly(s);
			}
			return firstContact;
		});
	}

	/**
	 * -load existing Customer -create a new Contact and add to customer's contacts
	 *
	 * @param customerId
	 * @return added Contact
	 */
	private Contact addContact(Integer customerId) throws Exception {
		assert customerId != null;
		return withTxSessionApply(s -> {
			final Customer customer = s.load(Customer.class, customerId);
			Contact contact = new Contact();
			contact.setName("contact name");
			contact.setTlf("wtf is tlf?");
			contact.setCustomer(customer);
			customer.getContacts().add(contact);
			// assuming contact is persisted via cascade from customer
			if (TERMINATE_ALL_USERS) {
				markRollbackOnly(s);
			}
			return contact;
		});
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

		withTxSession(s -> {
			Customer customer = s.load( Customer.class, customerId );
			Set<Contact> contacts = customer.getContacts();
			if ( contacts.size() != 1 ) {
				throw new IllegalStateException(
						"can't remove contact: customer id=" + customerId
								+ " expected exactly 1 contact, " + "actual count=" + contacts.size()
				);
			}

			Contact contact = contacts.iterator().next();
			// H2 version 1.3 (without MVCC fails with deadlock on Contacts/Customers modification, therefore,
			// we have to enforce locking Contacts first
			s.lock(contact, LockMode.PESSIMISTIC_WRITE);
			contacts.remove( contact );
			contact.setCustomer( null );

			// explicitly delete Contact because hbm has no 'DELETE_ORPHAN' cascade?
			// getEnvironment().getSessionFactory().getCurrentSession().delete(contact); //appears to
			// not be needed

			// assuming contact is persisted via cascade from customer

			if ( TERMINATE_ALL_USERS ) {
				markRollbackOnly(s);
			}
		});
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
			sb.append( r.toString() ).append( System.lineSeparator() );
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
						log.tracef( "Iteration completed %d", completedIterations );
					}
				}
			}
			catch (Throwable t) {
				TERMINATE_ALL_USERS = true;
				log.error( "Error", t );
				throw new Exception( t );
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
