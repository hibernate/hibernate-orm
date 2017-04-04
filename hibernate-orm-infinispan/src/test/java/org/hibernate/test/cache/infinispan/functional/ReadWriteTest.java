/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Cache;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.cache.infinispan.functional.entities.Citizen;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.functional.entities.NaturalIdOnManyToOne;
import org.hibernate.test.cache.infinispan.functional.entities.OtherItem;
import org.hibernate.test.cache.infinispan.functional.entities.State;
import org.hibernate.test.cache.infinispan.functional.entities.VersionedItem;
import org.junit.After;
import org.junit.Test;

import org.infinispan.commons.util.ByRef;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional entity transactional tests.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ReadWriteTest extends ReadOnlyTest {
	@Override
	public List<Object[]> getParameters() {
		return getParameters(true, true, false, true);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Citizen.class, State.class,
				NaturalIdOnManyToOne.class
		};
	}

	@After
	public void cleanupData() throws Exception {
		super.cleanupCache();
		withTxSession(s -> {
			s.createQuery( "delete NaturalIdOnManyToOne" ).executeUpdate();
			s.createQuery( "delete Citizen" ).executeUpdate();
			s.createQuery( "delete State" ).executeUpdate();
		});
	}

	@Test
	public void testCollectionCache() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		final Item another = new Item( "another", "Owned Item" );
		item.addItem( another );

		withTxSession(s -> {
			s.persist( item );
			s.persist( another );
		});
		// The collection has been removed, but we can't add it again immediately using putFromLoad
		TIME_SERVICE.advance(1);

		withTxSession(s -> {
			Item loaded = s.load( Item.class, item.getId() );
			assertEquals( 1, loaded.getItems().size() );
		});

		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
		assertEquals( 1, cStats.getElementCountInMemory() );

		withTxSession(s -> {
			Item loadedWithCachedCollection = (Item) s.load( Item.class, item.getId() );
			stats.logSummary();
			assertEquals( item.getName(), loadedWithCachedCollection.getName() );
			assertEquals( item.getItems().size(), loadedWithCachedCollection.getItems().size() );
			assertEquals( 1, cStats.getHitCount() );
			Map cacheEntries = cStats.getEntries();
			assertEquals( 1, cacheEntries.size() );
			Item itemElement = loadedWithCachedCollection.getItems().iterator().next();
			itemElement.setOwner( null );
			loadedWithCachedCollection.getItems().clear();
			s.delete( itemElement );
			s.delete( loadedWithCachedCollection );
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9231" )
	public void testAddNewOneToManyElementInitFlushLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		ByRef<Long> itemId = new ByRef<>(null);
		saveItem(itemId);

		// create an element for item.itsms
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		withTxSession(s -> {
			Item item = s.get( Item.class, itemId.get() );
			assertFalse( Hibernate.isInitialized( item.getItems() ) );
			// Add an element to item.items (a Set); it will initialize the Set.
			item.addItem( itemElement );
			assertTrue( Hibernate.isInitialized( item.getItems() ) );
			s.persist( itemElement );
			s.flush();
			markRollbackOnly(s);
		});

		withTxSession(s -> {
			Item item = s.get( Item.class, itemId.get() );
			Hibernate.initialize( item.getItems() );
			assertTrue( item.getItems().isEmpty() );
			s.delete( item );
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9231" )
	public void testAddNewOneToManyElementNoInitFlushLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		ByRef<Long> itemId = new ByRef<>(null);

		saveItem(itemId);

		// create an element for item.bagOfItems
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		withTxSession(s -> {
			Item item = s.get( Item.class, itemId.get() );
			assertFalse( Hibernate.isInitialized( item.getItems() ) );
			// Add an element to item.bagOfItems (a bag); it will not initialize the bag.
			item.addItemToBag( itemElement );
			assertFalse( Hibernate.isInitialized( item.getBagOfItems() ) );
			s.persist( itemElement );
			s.flush();
			markRollbackOnly(s);
		});

		withTxSession(s -> {
			Item item = s.get( Item.class, itemId.get() );
			Hibernate.initialize( item.getItems() );
			assertTrue( item.getItems().isEmpty() );
			s.delete( item );
		});
	}

	@Test
	public void testAddNewOneToManyElementNoInitFlushInitLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		ByRef<Long> itemId = new ByRef<>(null);

		saveItem(itemId);

		// create an element for item.bagOfItems
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		withTxSession(s -> {
			Item item = s.get(Item.class, itemId.get());
			assertFalse(Hibernate.isInitialized(item.getBagOfItems()));
			// Add an element to item.bagOfItems (a bag); it will not initialize the bag.
			item.addItemToBag(itemElement);
			assertFalse(Hibernate.isInitialized(item.getBagOfItems()));
			s.persist(itemElement);
			s.flush();
			// Now initialize the collection; it will contain the uncommitted itemElement.
			Hibernate.initialize(item.getBagOfItems());
			markRollbackOnly(s);
		});

		withTxSession(s -> {
			Item item = s.get(Item.class, itemId.get());
			// Because of HHH-9231, the following will fail due to ObjectNotFoundException because the
			// collection will be read from the cache and it still contains the uncommitted element,
			// which cannot be found.
			Hibernate.initialize(item.getBagOfItems());
			assertTrue(item.getBagOfItems().isEmpty());
			s.delete(item);
		});
	}

	protected void saveItem(ByRef<Long> itemId) throws Exception {
		withTxSession(s -> {
			Item item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			itemId.set(item.getId());
		});
	}

	@Test
	public void testAddNewManyToManyPropertyRefNoInitFlushInitLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		ByRef<Long> otherItemId = new ByRef<>(null);
		withTxSession(s -> {
			OtherItem otherItem = new OtherItem();
			otherItem.setName( "steve" );
			s.save( otherItem );
			otherItemId.set(otherItem.getId());
		});

		// create an element for otherItem.bagOfItems
		Item item = new Item();
		item.setName( "element" );
		item.setDescription( "element Item" );

		withTxSession(s -> {
			OtherItem otherItem = s.get( OtherItem.class, otherItemId.get() );
			assertFalse( Hibernate.isInitialized( otherItem.getBagOfItems() ) );
			// Add an element to otherItem.bagOfItems (a bag); it will not initialize the bag.
			otherItem.addItemToBag( item );
			assertFalse( Hibernate.isInitialized( otherItem.getBagOfItems() ) );
			s.persist( item );
			s.flush();
			// Now initialize the collection; it will contain the uncommitted itemElement.
			// The many-to-many uses a property-ref
			Hibernate.initialize( otherItem.getBagOfItems() );
			markRollbackOnly(s);
		});

		withTxSession(s -> {
			OtherItem otherItem = s.get( OtherItem.class, otherItemId.get() );
			// Because of HHH-9231, the following will fail due to ObjectNotFoundException because the
			// collection will be read from the cache and it still contains the uncommitted element,
			// which cannot be found.
			Hibernate.initialize( otherItem.getBagOfItems() );
			assertTrue( otherItem.getBagOfItems().isEmpty() );
			s.delete( otherItem );
		});
	}

	@Test
	public void testStaleWritesLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		ByRef<VersionedItem> itemRef = new ByRef<>(null);
		withTxSession(s -> {
			VersionedItem item = new VersionedItem();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			itemRef.set(item);
		});

		final VersionedItem item = itemRef.get();
		Long initialVersion = item.getVersion();

		// manually revert the version property
		item.setVersion( new Long( item.getVersion().longValue() - 1 ) );

		try {
			withTxSession(s -> s.update(item));
			fail("expected stale write to fail");
		}
		catch (Exception e) {
			log.debug("Rollback was expected", e);
		}

		// check the version value in the cache...
		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( VersionedItem.class.getName() );

		Object entry = slcs.getEntries().get( item.getId() );
		Long cachedVersionValue;
		cachedVersionValue = (Long) ((CacheEntry) entry).getVersion();
		assertEquals(initialVersion.longValue(), cachedVersionValue.longValue());

		withTxSession(s -> {
			VersionedItem item2 = s.load( VersionedItem.class, item.getId() );
			s.delete( item2 );
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5690")
	public void testPersistEntityFlushRollbackNotInEntityCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );

		ByRef<Long> itemId = new ByRef<>(null);
		withTxSession(s -> {
			Item item = new Item();
			item.setName("steve");
			item.setDescription("steve's item");
			s.persist(item);
			s.flush();
			itemId.set(item.getId());
//			assertNotNull( slcs.getEntries().get( item.getId() ) );
			markRollbackOnly(s);
		});

		// item should not be in entity cache.
		assertEquals( Collections.EMPTY_MAP, slcs.getEntries() );

		withTxSession(s -> {
			Item item = s.get( Item.class, itemId.get() );
			assertNull( item );
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5690")
	public void testPersistEntityFlushEvictGetRollbackNotInEntityCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );

		ByRef<Long> itemId = new ByRef<>(null);
		withTxSession(s -> {
			Item item = new Item();
			item.setName("steve");
			item.setDescription("steve's item");
			s.persist(item);
			s.flush();
			itemId.set(item.getId());
			// item is cached on insert.
//			assertNotNull( slcs.getEntries().get( item.getId() ) );
			s.evict(item);
			assertEquals(slcs.getHitCount(), 0);
			item = s.get(Item.class, item.getId());
			assertNotNull(item);
//			assertEquals( slcs.getHitCount(), 1 );
//			assertNotNull( slcs.getEntries().get( item.getId() ) );
			markRollbackOnly(s);
		});

		// item should not be in entity cache.
		//slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );
		assertEquals(Collections.EMPTY_MAP, slcs.getEntries() );

		withTxSession(s -> {
			Item item = s.get(Item.class, itemId.get());
			assertNull(item);
		});
	}

	@Test
	public void testQueryCacheInvalidation() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );
		sessionFactory().getCache().evictEntityRegion( Item.class.getName() );

		TIME_SERVICE.advance(1);

		assertEquals(0, slcs.getPutCount());
		assertEquals( 0, slcs.getElementCountInMemory() );
		assertEquals( 0, slcs.getEntries().size() );

		ByRef<Long> idRef = new ByRef<>(null);
		withTxSession(s -> {
			Item item = new Item();
			item.setName( "widget" );
			item.setDescription( "A really top-quality, full-featured widget." );
			s.persist( item );
			idRef.set( item.getId() );
		});

		assertEquals( 1, slcs.getPutCount() );
		assertEquals( 1, slcs.getElementCountInMemory() );
		assertEquals( 1, slcs.getEntries().size() );

		withTxSession(s -> {
			Item item = s.get( Item.class, idRef.get() );
			assertEquals( slcs.getHitCount(), 1 );
			assertEquals( slcs.getMissCount(), 0 );
			item.setDescription( "A bog standard item" );
		});

		assertEquals( slcs.getPutCount(), 2 );

		CacheEntry entry = (CacheEntry) slcs.getEntries().get( idRef.get() );
		Serializable[] ser = entry.getDisassembledState();
		assertTrue( ser[0].equals( "widget" ) );
		assertTrue( ser[1].equals( "A bog standard item" ) );

		withTxSession(s -> {
			Item item = s.load(Item.class, idRef.get());
			s.delete(item);
		});
	}

	@Test
	public void testQueryCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Item item = new Item( "chris", "Chris's Item" );

		withTxSession(s -> s.persist( item ));

		// Delay added to guarantee that query cache results won't be considered
		// as not up to date due to persist session and query results from first
		// query happening simultaneously.
		TIME_SERVICE.advance(1);

		withTxSession(s -> s.createQuery( "from Item" ).setCacheable( true ).list());

		withTxSession(s -> {
			s.createQuery( "from Item" ).setCacheable( true ).list();
			assertEquals( 1, stats.getQueryCacheHitCount() );
			s.createQuery( "delete from Item" ).executeUpdate();
		});
	}

	@Test
	public void testQueryCacheHitInSameTransaction() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Item item = new Item( "galder", "Galder's Item" );

		withTxSession(s -> s.persist( item ));

		// Delay added to guarantee that query cache results won't be considered
		// as not up to date due to persist session and query results from first
		// query happening simultaneously.
		TIME_SERVICE.advance(1);

		withTxSession(s -> {
			s.createQuery("from Item").setCacheable(true).list();
			s.createQuery("from Item").setCacheable(true).list();
			assertEquals(1, stats.getQueryCacheHitCount());
		});

		withTxSession(s -> s.createQuery( "delete from Item" ).executeUpdate());
	}

	@Test
	public void testNaturalIdCached() throws Exception {
		saveSomeCitizens();

		// Clear the cache before the transaction begins
		cleanupCache();
		TIME_SERVICE.advance(1);

		withTxSession(s -> {
			State france = ReadWriteTest.this.getState(s, "Ile de France");
			Criteria criteria = s.createCriteria( Citizen.class );
			criteria.add( Restrictions.naturalId().set( "ssn", "1234" ).set( "state", france ) );
			criteria.setCacheable( true );

			Statistics stats = sessionFactory().getStatistics();
			stats.setStatisticsEnabled( true );
			stats.clear();
			assertEquals(
					"Cache hits should be empty", 0, stats
					.getNaturalIdCacheHitCount()
			);

			// first query
			List results = criteria.list();
			assertEquals( 1, results.size() );
			assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
			assertEquals( "NaturalId Cache Misses", 1, stats.getNaturalIdCacheMissCount() );
			assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
			assertEquals( "NaturalId Cache Queries", 1, stats.getNaturalIdQueryExecutionCount() );

			// query a second time - result should be cached in session
			criteria.list();
			assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
			assertEquals( "NaturalId Cache Misses", 1, stats.getNaturalIdCacheMissCount() );
			assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
			assertEquals( "NaturalId Cache Queries", 1, stats.getNaturalIdQueryExecutionCount() );

			// cleanup
			markRollbackOnly(s);
		});
	}

	@Test
	public void testNaturalIdLoaderCached() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		saveSomeCitizens();

		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 2, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		//Try NaturalIdLoadAccess after insert
		final Citizen citizen = withTxSessionApply(s -> {
			State france = ReadWriteTest.this.getState(s, "Ile de France");
			NaturalIdLoadAccess<Citizen> naturalIdLoader = s.byNaturalId(Citizen.class);
			naturalIdLoader.using("ssn", "1234").using("state", france);

			//Not clearing naturalId caches, should be warm from entity loading
			stats.clear();

			// first query
			Citizen c = naturalIdLoader.load();
			assertNotNull(c);
			assertEquals("NaturalId Cache Hits", 1, stats.getNaturalIdCacheHitCount());
			assertEquals("NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount());
			assertEquals("NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount());
			assertEquals("NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount());

			// cleanup
			markRollbackOnly(s);
			return c;
		});

		// TODO: Clear caches manually via cache manager (it's faster!!)
		cleanupCache();
		TIME_SERVICE.advance(1);
		stats.setStatisticsEnabled( true );
		stats.clear();

		//Try NaturalIdLoadAccess
		withTxSession(s -> {
			// first query
			Citizen loadedCitizen = (Citizen) s.get( Citizen.class, citizen.getId() );
			assertNotNull( loadedCitizen );
			assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
			assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
			assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
			assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

			// cleanup
			markRollbackOnly(s);
		});

		// Try NaturalIdLoadAccess after load
		withTxSession(s -> {
			State france = ReadWriteTest.this.getState(s, "Ile de France");
			NaturalIdLoadAccess naturalIdLoader = s.byNaturalId(Citizen.class);
			naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

			//Not clearing naturalId caches, should be warm from entity loading
			stats.setStatisticsEnabled( true );
			stats.clear();

			// first query
			Citizen loadedCitizen = (Citizen) naturalIdLoader.load();
			assertNotNull( loadedCitizen );
			assertEquals( "NaturalId Cache Hits", 1, stats.getNaturalIdCacheHitCount() );
			assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
			assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
			assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

			// cleanup
			markRollbackOnly(s);
		});

	}

	@Test
	public void testEntityCacheContentsAfterEvictAll() throws Exception {
		final List<Citizen> citizens = saveSomeCitizens();

		withTxSession(s -> {
			Cache cache = s.getSessionFactory().getCache();

			Statistics stats = sessionFactory().getStatistics();
			SecondLevelCacheStatistics slcStats = stats.getSecondLevelCacheStatistics(Citizen.class.getName());

			assertTrue("2lc entity cache is expected to contain Citizen id = " + citizens.get(0).getId(),
					cache.containsEntity(Citizen.class, citizens.get(0).getId()));
			assertTrue("2lc entity cache is expected to contain Citizen id = " + citizens.get(1).getId(),
					cache.containsEntity(Citizen.class, citizens.get(1).getId()));
			assertEquals(2, slcStats.getPutCount());

			cache.evictEntityRegions();
			TIME_SERVICE.advance(1);

			assertEquals(0, slcStats.getElementCountInMemory());
			assertFalse("2lc entity cache is expected to not contain Citizen id = " + citizens.get(0).getId(),
					cache.containsEntity(Citizen.class, citizens.get(0).getId()));
			assertFalse("2lc entity cache is expected to not contain Citizen id = " + citizens.get(1).getId(),
					cache.containsEntity(Citizen.class, citizens.get(1).getId()));

			Citizen citizen = s.load(Citizen.class, citizens.get(0).getId());
			assertNotNull(citizen);
			assertNotNull(citizen.getFirstname()); // proxy gets resolved
			assertEquals(1, slcStats.getMissCount());

			// cleanup
			markRollbackOnly(s);
		});
	}

	@Test
	public void testMultipleEvictAll() throws Exception {
		final List<Citizen> citizens = saveSomeCitizens();

		withTxSession(s -> {
			Cache cache = s.getSessionFactory().getCache();

			cache.evictEntityRegions();
			cache.evictEntityRegions();
		});
		withTxSession(s -> {
			Cache cache = s.getSessionFactory().getCache();

			cache.evictEntityRegions();

			s.delete(s.load(Citizen.class, citizens.get(0).getId()));
			s.delete(s.load(Citizen.class, citizens.get(1).getId()));
		});
	}

	private List<Citizen> saveSomeCitizens() throws Exception {
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

		withTxSession(s -> {
			s.persist( australia );
			s.persist( france );
			s.persist( c1 );
			s.persist( c2 );
		});

		List<Citizen> citizens = new ArrayList<>(2);
		citizens.add(c1);
		citizens.add(c2);
		return citizens;
	}

	private State getState(Session s, String name) {
		Criteria criteria = s.createCriteria( State.class );
		criteria.add( Restrictions.eq("name", name) );
		criteria.setCacheable(true);
		return (State) criteria.list().get( 0 );
	}

}
