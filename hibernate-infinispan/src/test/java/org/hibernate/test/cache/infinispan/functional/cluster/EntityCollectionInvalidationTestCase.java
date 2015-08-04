/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import javax.transaction.TransactionManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.functional.Contact;
import org.hibernate.test.cache.infinispan.functional.Customer;
import org.hibernate.testing.TestForIssue;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commands.read.GetKeyValueCommand;
import org.infinispan.commons.util.Util;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.util.collection.ConcurrentSet;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * EntityCollectionInvalidationTestCase.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityCollectionInvalidationTestCase extends DualNodeTestCase {
	private static final Log log = LogFactory.getLog( EntityCollectionInvalidationTestCase.class );

	private static final long SLEEP_TIME = 50l;
	private static final Integer CUSTOMER_ID = new Integer( 1 );

	static int test = 0;

	private EmbeddedCacheManager localManager, remoteManager;
	private Cache localCustomerCache, remoteCustomerCache;
	private Cache localContactCache, remoteContactCache;
	private Cache localCollectionCache, remoteCollectionCache;
	private MyListener localListener, remoteListener;
	private TransactionManager localTM, remoteTM;
	private SessionFactory localFactory, remoteFactory;

	@Override
	public void startUp()  {
		super.startUp();
		// Bind a listener to the "local" cache
		// Our region factory makes its CacheManager available to us
		localManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.LOCAL );
		// Cache localCache = localManager.getCache("entity");
		localCustomerCache = localManager.getCache( Customer.class.getName() );
		localContactCache = localManager.getCache( Contact.class.getName() );
		localCollectionCache = localManager.getCache( Customer.class.getName() + ".contacts" );
		localListener = new MyListener( "local" );
		localCustomerCache.addListener( localListener );
		localContactCache.addListener( localListener );
		localCollectionCache.addListener( localListener );

		// Bind a listener to the "remote" cache
		remoteManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.REMOTE );
		remoteCustomerCache = remoteManager.getCache( Customer.class.getName() );
		remoteContactCache = remoteManager.getCache( Contact.class.getName() );
		remoteCollectionCache = remoteManager.getCache( Customer.class.getName() + ".contacts" );
		remoteListener = new MyListener( "remote" );
		remoteCustomerCache.addListener( remoteListener );
		remoteContactCache.addListener( remoteListener );
		remoteCollectionCache.addListener( remoteListener );

		localFactory = sessionFactory();
		remoteFactory = secondNodeEnvironment().getSessionFactory();

		localTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.LOCAL );
		remoteTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.REMOTE );
	}

	@Override
	public void shutDown() {
		cleanupTransactionManagement();
	}

	@Override
	protected void cleanupTest() throws Exception {
      cleanup(localFactory, localTM);
      localListener.clear();
      remoteListener.clear();
		// do not call super.cleanupTest becasue we would clean transaction managers
	}

	@Test
	public void testAll() throws Exception {
		assertEmptyCaches();
		assertTrue( remoteListener.isEmpty() );
		assertTrue( localListener.isEmpty() );

		log.debug( "Create node 0" );
		IdContainer ids = createCustomer( localFactory, localTM );

		assertTrue( remoteListener.isEmpty() );
		assertTrue( localListener.isEmpty() );

		// Sleep a bit to let async commit propagate. Really just to
		// help keep the logs organized for debugging any issues
		sleep( SLEEP_TIME );

		log.debug( "Find node 0" );
		// This actually brings the collection into the cache
		getCustomer( ids.customerId, localFactory, localTM );

		sleep( SLEEP_TIME );

		// Now the collection is in the cache so, the 2nd "get"
		// should read everything from the cache
		log.debug( "Find(2) node 0" );
		localListener.clear();
		getCustomer( ids.customerId, localFactory, localTM );

		// Check the read came from the cache
		log.debug( "Check cache 0" );
		assertLoadedFromCache( localListener, ids.customerId, ids.contactIds );

		log.debug( "Find node 1" );
		// This actually brings the collection into the cache since invalidation is in use
		getCustomer( ids.customerId, remoteFactory, remoteTM );

		// Now the collection is in the cache so, the 2nd "get"
		// should read everything from the cache
		log.debug( "Find(2) node 1" );
		remoteListener.clear();
		getCustomer( ids.customerId, remoteFactory, remoteTM );

		// Check the read came from the cache
		log.debug( "Check cache 1" );
		assertLoadedFromCache( remoteListener, ids.customerId, ids.contactIds );

		// Modify customer in remote
		remoteListener.clear();
		ids = modifyCustomer( ids.customerId, remoteFactory, remoteTM );
		sleep( 250 );
		assertLoadedFromCache( remoteListener, ids.customerId, ids.contactIds );

		// After modification, local cache should have been invalidated and hence should be empty
		assertEquals( 0, localCollectionCache.size() );
		assertEquals( 0, localCustomerCache.size() );
	}

	@TestForIssue(jiraKey = "HHH-9881")
	@Test
	public void testConcurrentLoadAndRemoval() throws Exception {
		AtomicReference<Exception> getException = new AtomicReference<>();
		AtomicReference<Exception> deleteException = new AtomicReference<>();

		Phaser getPhaser = new Phaser(2);
		HookInterceptor hookInterceptor = new HookInterceptor(getException);
		AdvancedCache remotePPCache = remoteCustomerCache.getCacheManager().getCache(
				remoteCustomerCache.getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME).getAdvancedCache();
		remotePPCache.getAdvancedCache().addInterceptor(hookInterceptor, 0);

		IdContainer idContainer = new IdContainer();
		withTx(localTM, () -> {
			Session s = localFactory.getCurrentSession();
			s.getTransaction().begin();
			Customer customer = new Customer();
			customer.setName( "JBoss" );
			s.persist(customer);
			s.getTransaction().commit();
			s.close();
			idContainer.customerId = customer.getId();
			return null;
		});
		// start loading

		Thread getThread = new Thread(() -> {
			try {
				withTx(remoteTM, () -> {
					Session s = remoteFactory.getCurrentSession();
					s.getTransaction().begin();
					s.get(Customer.class, idContainer.customerId);
					s.getTransaction().commit();
					s.close();
					return null;
				});
			} catch (Exception e) {
				log.error("Failure to get customer", e);
				getException.set(e);
			}
		}, "get-thread");
		Thread deleteThread = new Thread(() -> {
			try {
				withTx(localTM, () -> {
					Session s = localFactory.getCurrentSession();
					s.getTransaction().begin();
					Customer customer = s.get(Customer.class, idContainer.customerId);
					s.delete(customer);
					s.getTransaction().commit();
					return null;
				});
			} catch (Exception e) {
				log.error("Failure to delete customer", e);
				deleteException.set(e);
			}
		}, "delete-thread");
		// get thread should block on the beginning of PutFromLoadValidator#acquirePutFromLoadLock
		hookInterceptor.block(getPhaser, getThread);
		getThread.start();

		arriveAndAwait(getPhaser);
		deleteThread.start();
		deleteThread.join();
		hookInterceptor.unblock();
		arriveAndAwait(getPhaser);
		getThread.join();

		if (getException.get() != null) {
			throw new IllegalStateException("get-thread failed", getException.get());
		}
		if (deleteException.get() != null) {
			throw new IllegalStateException("delete-thread failed", deleteException.get());
		}

		Customer localCustomer = getCustomer(idContainer.customerId, localFactory, localTM);
		assertNull(localCustomer);
		Customer remoteCustomer = getCustomer(idContainer.customerId, remoteFactory, remoteTM);
		assertNull(remoteCustomer);
		assertTrue(remoteCustomerCache.isEmpty());
	}

	protected void assertEmptyCaches() {
		assertTrue( localCustomerCache.isEmpty() );
		assertTrue( localContactCache.isEmpty() );
		assertTrue( localCollectionCache.isEmpty() );
		assertTrue( remoteCustomerCache.isEmpty() );
		assertTrue( remoteContactCache.isEmpty() );
		assertTrue( remoteCollectionCache.isEmpty() );
	}

	private IdContainer createCustomer(SessionFactory sessionFactory, TransactionManager tm)
			throws Exception {
		log.debug( "CREATE CUSTOMER" );

		tm.begin();

		try {
			Session session = sessionFactory.getCurrentSession();
			Customer customer = new Customer();
			customer.setName( "JBoss" );
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

			session.save( customer );
			tm.commit();

			IdContainer ids = new IdContainer();
			ids.customerId = customer.getId();
			Set contactIds = new HashSet();
			contactIds.add( kabir.getId() );
			contactIds.add( bill.getId() );
			ids.contactIds = contactIds;

			return ids;
		}
		catch (Exception e) {
			log.error( "Caught exception creating customer", e );
			try {
				tm.rollback();
			}
			catch (Exception e1) {
				log.error( "Exception rolling back txn", e1 );
			}
			throw e;
		}
		finally {
			log.debug( "CREATE CUSTOMER -  END" );
		}
	}

	private Customer getCustomer(Integer id, SessionFactory sessionFactory, TransactionManager tm) throws Exception {
		log.debug( "Find customer with id=" + id );
		tm.begin();
		try {
			Session session = sessionFactory.getCurrentSession();
			Customer customer = doGetCustomer( id, session, tm );
			tm.commit();
			return customer;
		}
		catch (Exception e) {
			try {
				tm.rollback();
			}
			catch (Exception e1) {
				log.error( "Exception rolling back txn", e1 );
			}
			throw e;
		}
		finally {
			log.debug( "Find customer ended." );
		}
	}

	private Customer doGetCustomer(Integer id, Session session, TransactionManager tm) throws Exception {
		Customer customer = session.get( Customer.class, id );
		if (customer == null) {
			return null;
		}
		// Access all the contacts
		Set<Contact> contacts = customer.getContacts();
		if (contacts != null) {
			for (Iterator it = contacts.iterator(); it.hasNext(); ) {
				((Contact) it.next()).getName();
			}
		}
		return customer;
	}

	private IdContainer modifyCustomer(Integer id, SessionFactory sessionFactory, TransactionManager tm)
			throws Exception {
		log.debug( "Modify customer with id=" + id );
		tm.begin();
		try {
			Session session = sessionFactory.getCurrentSession();
			IdContainer ids = new IdContainer();
			Set contactIds = new HashSet();
			Customer customer = doGetCustomer( id, session, tm );
			customer.setName( "NewJBoss" );
			ids.customerId = customer.getId();
			Set<Contact> contacts = customer.getContacts();
			for ( Contact c : contacts ) {
				contactIds.add( c.getId() );
			}
			Contact contact = contacts.iterator().next();
			contacts.remove( contact );
			contactIds.remove( contact.getId() );
			ids.contactIds = contactIds;
			contact.setCustomer( null );

			session.save( customer );
			tm.commit();
			return ids;
		}
		catch (Exception e) {
			try {
				tm.rollback();
			}
			catch (Exception e1) {
				log.error( "Exception rolling back txn", e1 );
			}
			throw e;
		}
		finally {
			log.debug( "Find customer ended." );
		}
	}

	private void cleanup(SessionFactory sessionFactory, TransactionManager tm) throws Exception {
		tm.begin();
		try {
			Session session = sessionFactory.getCurrentSession();
			Customer c = (Customer) session.get( Customer.class, CUSTOMER_ID );
			if ( c != null ) {
				Set contacts = c.getContacts();
				for ( Iterator it = contacts.iterator(); it.hasNext(); ) {
					session.delete( it.next() );
				}
				c.setContacts( null );
				session.delete( c );
			}
			// since we don't use orphan removal, some contacts may persist
			for (Object contact : session.createCriteria(Contact.class).list()) {
				session.delete(contact);
			}
			tm.commit();
		}
		catch (Exception e) {
			try {
				tm.rollback();
			}
			catch (Exception e1) {
				log.error( "Exception rolling back txn", e1 );
			}
			log.error( "Caught exception in cleanup", e );
		}
	}

	private void assertLoadedFromCache(MyListener listener, Integer custId, Set contactIds) {
		assertTrue(
				"Customer#" + custId + " was in cache", listener.visited.contains(
				"Customer#"
						+ custId
		)
		);
		for ( Iterator it = contactIds.iterator(); it.hasNext(); ) {
			Integer contactId = (Integer) it.next();
			assertTrue(
					"Contact#" + contactId + " was in cache", listener.visited.contains(
					"Contact#"
							+ contactId
			)
			);
			assertTrue(
					"Contact#" + contactId + " was in cache", listener.visited.contains(
					"Contact#"
							+ contactId
			)
			);
		}
		assertTrue(
				"Customer.contacts" + custId + " was in cache", listener.visited
				.contains( "Customer.contacts#" + custId )
		);
	}

	protected static void arriveAndAwait(Phaser phaser) throws TimeoutException, InterruptedException {
		try {
			phaser.awaitAdvanceInterruptibly(phaser.arrive(), 10, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.error("Failed to progress: " + Util.threadDump());
			throw e;
		}
	}

	@Listener
	public static class MyListener {
		private static final Log log = LogFactory.getLog( MyListener.class );
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
			log.debug( event.toString() );
			if ( !event.isPre() ) {
				String key = event.getCache().getName() + "#" + event.getKey();
				log.debug( "MyListener[" + name + "] - Visiting key " + key );
				// String name = fqn.toString();
				String token = ".functional.";
				int index = key.indexOf( token );
				if ( index > -1 ) {
					index += token.length();
					key = key.substring( index );
					log.debug( "MyListener[" + this.name + "] - recording visit to " + key );
					visited.add( key );
				}
			}
		}
	}

	private class IdContainer {
		Integer customerId;
		Set<Integer> contactIds;
	}

	private static class HookInterceptor extends BaseCustomInterceptor {
		final AtomicReference<Exception> failure;
		Phaser phaser;
		Thread thread;

		private HookInterceptor(AtomicReference<Exception> failure) {
			this.failure = failure;
		}

		public synchronized void block(Phaser phaser, Thread thread) {
			this.phaser = phaser;
			this.thread = thread;
		}

		public synchronized void unblock() {
			phaser = null;
			thread = null;
		}

		@Override
		public Object visitGetKeyValueCommand(InvocationContext ctx, GetKeyValueCommand command) throws Throwable {
			try {
				Phaser phaser;
				Thread thread;
				synchronized (this) {
					phaser = this.phaser;
					thread = this.thread;
				}
				if (phaser != null && Thread.currentThread() == thread) {
					arriveAndAwait(phaser);
					arriveAndAwait(phaser);
				}
			} catch (Exception e) {
				failure.set(e);
				throw e;
			} finally {
				return super.visitGetKeyValueCommand(ctx, command);
			}
		}
	}

}
