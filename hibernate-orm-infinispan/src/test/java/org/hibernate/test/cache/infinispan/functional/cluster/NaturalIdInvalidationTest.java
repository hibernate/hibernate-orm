/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.test.cache.infinispan.functional.entities.Citizen;
import org.hibernate.test.cache.infinispan.functional.entities.NaturalIdOnManyToOne;
import org.hibernate.test.cache.infinispan.functional.entities.State;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.jboss.util.collection.ConcurrentSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 */
public class NaturalIdInvalidationTest extends DualNodeTest {

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(NaturalIdInvalidationTest.class);

	@Override
	public List<Object[]> getParameters() {
		return getParameters(true, true, true, true);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Citizen.class, State.class,
				NaturalIdOnManyToOne.class
		};
	}

	@Test
	public void testAll() throws Exception {
		log.info( "*** testAll()" );

		// Bind a listener to the "local" cache
		// Our region factory makes its CacheManager available to us
		CacheContainer localManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTest.LOCAL);
		Cache localNaturalIdCache = localManager.getCache(Citizen.class.getName() + "##NaturalId");
		MyListener localListener = new MyListener( "local" );
		localNaturalIdCache.addListener(localListener);

		// Bind a listener to the "remote" cache
		CacheContainer remoteManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTest.REMOTE);
		Cache remoteNaturalIdCache = remoteManager.getCache(Citizen.class.getName() + "##NaturalId");
		MyListener remoteListener = new MyListener( "remote" );
		remoteNaturalIdCache.addListener(remoteListener);

		SessionFactory localFactory = sessionFactory();
		SessionFactory remoteFactory = secondNodeEnvironment().getSessionFactory();

		try {
			assertTrue(remoteListener.isEmpty());
			assertTrue(localListener.isEmpty());

			CountDownLatch remoteUpdateLatch = expectAfterUpdate(remoteNaturalIdCache.getAdvancedCache(), 2);
			saveSomeCitizens(localFactory);

			assertTrue(remoteUpdateLatch.await(2, TimeUnit.SECONDS));

			assertTrue(remoteListener.isEmpty());
			assertTrue(localListener.isEmpty());

			log.debug("Find node 0");
			// This actually brings the collection into the cache
			getCitizenWithCriteria(localFactory);

			// Now the collection is in the cache so, the 2nd "get"
			// should read everything from the cache
			log.debug( "Find(2) node 0" );
			localListener.clear();
			getCitizenWithCriteria(localFactory);

			// Check the read came from the cache
			log.debug( "Check cache 0" );
			assertLoadedFromCache(localListener, "1234");

			log.debug( "Find node 1" );
			// This actually brings the collection into the cache since invalidation is in use
			getCitizenWithCriteria(remoteFactory);

			// Now the collection is in the cache so, the 2nd "get"
			// should read everything from the cache
			log.debug( "Find(2) node 1" );
			remoteListener.clear();
			getCitizenWithCriteria(remoteFactory);

			// Check the read came from the cache
			log.debug( "Check cache 1" );
			assertLoadedFromCache(remoteListener, "1234");

			// Modify customer in remote
			remoteListener.clear();
			CountDownLatch localUpdate = expectEvict(localNaturalIdCache.getAdvancedCache(), 1);
			deleteCitizenWithCriteria(remoteFactory);
			assertTrue(localUpdate.await(2, TimeUnit.SECONDS));

			Set localKeys = localNaturalIdCache.keySet();
			assertEquals(1, localKeys.size());
			// Only key left is the one for the citizen *not* in France
			localKeys.toString().contains("000");
		}
		catch (Exception e) {
			log.error("Error", e);
			throw e;
		} finally {
			withTxSession(localFactory, s -> {
				s.createQuery( "delete NaturalIdOnManyToOne" ).executeUpdate();
				s.createQuery( "delete Citizen" ).executeUpdate();
				s.createQuery( "delete State" ).executeUpdate();
			});
		}
	}

	private void assertLoadedFromCache(MyListener localListener, String id) {
		for (String visited : localListener.visited){
			if (visited.contains(id))
				return;
		}
		fail("Citizen (" + id + ") should have present in the cache");
	}

	private void saveSomeCitizens(SessionFactory sf) throws Exception {
		final Citizen c1 = new Citizen();
		c1.setFirstname( "Emmanuel" );
		c1.setLastname( "Bernard" );
		c1.setSsn( "1234" );

		final State france = new State();
		france.setName( "Ile de France" );
		c1.setState( france );

		final Citizen c2 = new Citizen();
		c2.setFirstname( "Gavin" );
		c2.setLastname( "King" );
		c2.setSsn( "000" );
		final State australia = new State();
		australia.setName( "Australia" );
		c2.setState( australia );

		withTxSession(sf, s -> {
			s.persist( australia );
			s.persist( france );
			s.persist( c1 );
			s.persist( c2 );
		});
	}

	private void getCitizenWithCriteria(SessionFactory sf) throws Exception {
		withTxSession(sf, s -> {
			State france = getState(s, "Ile de France");
			Criteria criteria = s.createCriteria( Citizen.class );
			criteria.add( Restrictions.naturalId().set( "ssn", "1234" ).set( "state", france ) );
			criteria.setCacheable( true );
			criteria.list();
		});
	}

	private void deleteCitizenWithCriteria(SessionFactory sf) throws Exception {
		withTxSession(sf, s -> {
			State france = getState(s, "Ile de France");
			Criteria criteria = s.createCriteria( Citizen.class );
			criteria.add( Restrictions.naturalId().set( "ssn", "1234" ).set( "state", france ) );
			criteria.setCacheable( true );
			Citizen c = (Citizen) criteria.uniqueResult();
			s.delete(c);
		});
	}

	private State getState(Session s, String name) {
		Criteria criteria = s.createCriteria( State.class );
		criteria.add( Restrictions.eq("name", name) );
		criteria.setCacheable(true);
		return (State) criteria.list().get( 0 );
	}

	@Listener
	public static class MyListener {
		private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( MyListener.class );
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
				visited.add(event.getKey().toString());
			}
		}
	}

}
