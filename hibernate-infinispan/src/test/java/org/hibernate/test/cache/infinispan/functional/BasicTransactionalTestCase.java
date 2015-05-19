/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hibernate.Cache;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.infinispan.test.TestingUtil.withTx;
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
public class BasicTransactionalTestCase extends AbstractFunctionalTestCase {
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
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = sessionFactory().openSession();
            s.beginTransaction();
            s.createQuery( "delete NaturalIdOnManyToOne" ).executeUpdate();
            s.createQuery( "delete Citizen" ).executeUpdate();
            s.createQuery( "delete State" ).executeUpdate();
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
   }

	@Test
	public void testCollectionCache() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		final Item another = new Item( "another", "Owned Item" );
		item.addItem( another );

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            s.persist( item );
            s.persist( another );
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Item loaded = (Item) s.load( Item.class, item.getId() );
            assertEquals( 1, loaded.getItems().size() );
            s.close();
            return null;
         }
      });

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
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
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9231" )
	public void testAddNewOneToManyElementInitFlushLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		Item item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// create an element for item.itsms
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			assertFalse( Hibernate.isInitialized( item.getItems() ) );
			// Add an element to item.items (a Set); it will initialize the Set.
			item.addItem( itemElement );
			assertTrue( Hibernate.isInitialized( item.getItems() ) );
			s.persist( itemElement );
			s.flush();
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTxExpected(e);
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		beginTx();
		try {
			// cleanup
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			Hibernate.initialize( item.getItems() );
			assertTrue( item.getItems().isEmpty() );
			s.delete( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9231" )
	public void testAddNewOneToManyElementNoInitFlushLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		Item item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// create an element for item.bagOfItems
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			assertFalse( Hibernate.isInitialized( item.getItems() ) );
			// Add an element to item.bagOfItems (a bag); it will not initialize the bag.
			item.addItemToBag( itemElement );
			assertFalse( Hibernate.isInitialized( item.getBagOfItems() ) );
			s.persist( itemElement );
			s.flush();
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTxExpected(e);
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		beginTx();
		try {
			// cleanup
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			Hibernate.initialize( item.getItems() );
			assertTrue( item.getItems().isEmpty() );
			s.delete( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testAddNewOneToManyElementNoInitFlushInitLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		Item item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// create an element for item.bagOfItems
		Item itemElement = new Item();
		itemElement.setName( "element" );
		itemElement.setDescription( "element item" );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			assertFalse( Hibernate.isInitialized( item.getBagOfItems() ) );
			// Add an element to item.bagOfItems (a bag); it will not initialize the bag.
			item.addItemToBag( itemElement );
			assertFalse( Hibernate.isInitialized( item.getBagOfItems() ) );
			s.persist( itemElement );
			s.flush();
			// Now initialize the collection; it will contain the uncommitted itemElement.
			Hibernate.initialize( item.getBagOfItems() );
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTxExpected(e);
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		beginTx();
		try {
			// cleanup
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			// Because of HHH-9231, the following will fail due to ObjectNotFoundException because the
			// collection will be read from the cache and it still contains the uncommitted element,
			// which cannot be found.
			Hibernate.initialize( item.getBagOfItems() );
			assertTrue( item.getBagOfItems().isEmpty() );
			s.delete( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testAddNewManyToManyPropertyRefNoInitFlushInitLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );

		OtherItem otherItem = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			otherItem = new OtherItem();
			otherItem.setName( "steve" );
			s.save( otherItem );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// create an element for otherItem.bagOfItems
		Item item = new Item();
		item.setName( "element" );
		item.setDescription( "element Item" );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			otherItem = (OtherItem) s.get( OtherItem.class, otherItem.getId() );
			assertFalse( Hibernate.isInitialized( otherItem.getBagOfItems() ) );
			// Add an element to otherItem.bagOfItems (a bag); it will not initialize the bag.
			otherItem.addItemToBag( item );
			assertFalse( Hibernate.isInitialized( otherItem.getBagOfItems() ) );
			s.persist( item );
			s.flush();
			// Now initialize the collection; it will contain the uncommitted itemElement.
			// The many-to-many uses a property-ref
			Hibernate.initialize( otherItem.getBagOfItems() );
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTxExpected(e);
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		beginTx();
		try {
			// cleanup
			s = openSession();
			txn = s.beginTransaction();
			otherItem = (OtherItem) s.get( OtherItem.class, otherItem.getId() );
			// Because of HHH-9231, the following will fail due to ObjectNotFoundException because the
			// collection will be read from the cache and it still contains the uncommitted element,
			// which cannot be found.
			Hibernate.initialize( otherItem.getBagOfItems() );
			assertTrue( otherItem.getBagOfItems().isEmpty() );
			s.delete( otherItem );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testStaleWritesLeaveCacheConsistent() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		VersionedItem item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new VersionedItem();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.save( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		Long initialVersion = item.getVersion();

		// manually revert the version property
		item.setVersion( new Long( item.getVersion().longValue() - 1 ) );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			s.update(item);
			txn.commit();
			fail("expected stale write to fail");
		}
		catch (Exception e) {
			setRollbackOnlyTxExpected(e);
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		// check the version value in the cache...
		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( VersionedItem.class.getName() );

		Object entry = slcs.getEntries().get( item.getId() );
		Long cachedVersionValue;
		cachedVersionValue = (Long) ((CacheEntry) entry).getVersion();
		assertEquals(initialVersion.longValue(), cachedVersionValue.longValue());

		beginTx();
		try {
			// cleanup
			s = openSession();
			txn = s.beginTransaction();
			item = (VersionedItem) s.load( VersionedItem.class, item.getId() );
			s.delete( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5690")
	public void testPersistEntityFlushRollbackNotInEntityCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );

		Item item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.persist( item );
			s.flush();
			assertNotNull( slcs.getEntries().get( item.getId() ) );
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		// item should not be in entity cache.
		assertTrue( slcs.getEntries().isEmpty() );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			assertNull( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5690")
	public void testPersistEntityFlushEvictGetRollbackNotInEntityCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );

		Item item = null;
		Transaction txn = null;
		Session s = null;

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = new Item();
			item.setName( "steve" );
			item.setDescription( "steve's item" );
			s.persist( item );
			s.flush();
			// item is cached on insert.
			assertNotNull( slcs.getEntries().get( item.getId() ) );
			s.evict( item );
			assertEquals( slcs.getHitCount(), 0 );
			item = (Item) s.get( Item.class, item.getId() );
			assertNotNull( item );
			assertEquals( slcs.getHitCount(), 1 );
			assertNotNull( slcs.getEntries().get( item.getId() ) );
			setRollbackOnlyTx();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}

		// item should not be in entity cache.
		//slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );
		assertTrue( slcs.getEntries().isEmpty() );

		beginTx();
		try {
			s = openSession();
			txn = s.beginTransaction();
			item = (Item) s.get( Item.class, item.getId() );
			assertNull( item );
			txn.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testQueryCacheInvalidation() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics( Item.class.getName() );
		sessionFactory().getCache().evictEntityRegion( Item.class.getName() );

		assertEquals(0, slcs.getPutCount());
		assertEquals( 0, slcs.getElementCountInMemory() );
		assertEquals( 0, slcs.getEntries().size() );

		Session s = null;
		Transaction t = null;
		Item i = null;

		beginTx();
		try {
			s = openSession();
			t = s.beginTransaction();
			i = new Item();
			i.setName( "widget" );
			i.setDescription( "A really top-quality, full-featured widget." );
			s.persist( i );
			t.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}


		assertEquals( 1, slcs.getPutCount() );
		assertEquals( 1, slcs.getElementCountInMemory() );
		assertEquals( 1, slcs.getEntries().size() );

		beginTx();
		try {
			s = openSession();
			t = s.beginTransaction();
			i = (Item) s.get( Item.class, i.getId() );
			assertEquals( slcs.getHitCount(), 1 );
			assertEquals( slcs.getMissCount(), 0 );
			i.setDescription( "A bog standard item" );
			t.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		assertEquals( slcs.getPutCount(), 2 );

		CacheEntry entry = (CacheEntry) slcs.getEntries().get( i.getId() );
		Serializable[] ser = entry.getDisassembledState();
		assertTrue( ser[0].equals( "widget" ) );
		assertTrue( ser[1].equals( "A bog standard item" ) );

		beginTx();
		try {
			// cleanup
			s = openSession();
			t = s.beginTransaction();
			s.delete(i);
			t.commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testQueryCache() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Session s;
		Item item = new Item( "chris", "Chris's Item" );

		beginTx();
		try {
			s = openSession();
			s.getTransaction().begin();
			s.persist( item );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// Delay added to guarantee that query cache results won't be considered
		// as not up to date due to persist session and query results from first
		// query happening within same 100ms gap.
		Thread.sleep( 100 );

		beginTx();
		try {
			s = openSession();
			s.createQuery( "from Item" ).setCacheable( true ).list();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		beginTx();
		try {
			s = openSession();
			s.createQuery( "from Item" ).setCacheable( true ).list();
			assertEquals( 1, stats.getQueryCacheHitCount() );
			s.createQuery( "delete from Item" ).executeUpdate();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	public void testQueryCacheHitInSameTransaction() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Session s = null;
		Item item = new Item( "galder", "Galder's Item" );

		beginTx();
		try {
			s = openSession();
			s.getTransaction().begin();
			s.persist( item );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// Delay added to guarantee that query cache results won't be considered
		// as not up to date due to persist session and query results from first
		// query happening within same 100ms gap.
		Thread.sleep( 100 );

		beginTx();
		try {
			s = openSession();
			s.createQuery( "from Item" ).setCacheable( true ).list();
			s.createQuery( "from Item" ).setCacheable( true ).list();
			assertEquals( 1, stats.getQueryCacheHitCount() );
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		beginTx();
		try {
			s = openSession();
			s.createQuery( "delete from Item" ).executeUpdate();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

   @Test
   public void testNaturalIdCached() throws Exception {
      saveSomeCitizens();

      // Clear the cache before the transaction begins
      BasicTransactionalTestCase.this.cleanupCache();

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();
            State france = BasicTransactionalTestCase.this.getState(s, "Ile de France");
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
            tx.rollback();
            s.close();
            return null;
         }
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
      final Citizen citizen = withTx(tm, new Callable<Citizen>() {
         @Override
         public Citizen call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();
            State france = BasicTransactionalTestCase.this.getState(s, "Ile de France");
            NaturalIdLoadAccess naturalIdLoader = s.byNaturalId(Citizen.class);
            naturalIdLoader.using("ssn", "1234").using("state", france);

            //Not clearing naturalId caches, should be warm from entity loading
            stats.clear();

            // first query
            Citizen citizen = (Citizen) naturalIdLoader.load();
            assertNotNull(citizen);
            assertEquals("NaturalId Cache Hits", 1, stats.getNaturalIdCacheHitCount());
            assertEquals("NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount());
            assertEquals("NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount());
            assertEquals("NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount());

            // cleanup
            tx.rollback();
            s.close();
            return citizen;
         }
      });

      // TODO: Clear caches manually via cache manager (it's faster!!)
      this.cleanupCache();
      Thread.sleep(PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD + TimeUnit.SECONDS.toMillis(1));
      stats.setStatisticsEnabled( true );
      stats.clear();

      //Try NaturalIdLoadAccess
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();

            // first query
            Citizen loadedCitizen = (Citizen) s.get( Citizen.class, citizen.getId() );
            assertNotNull( loadedCitizen );
            assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
            assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
            assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
            assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

            // cleanup
            tx.rollback();
            s.close();
            return null;
         }
      });

      // Try NaturalIdLoadAccess after load
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();
            State france = BasicTransactionalTestCase.this.getState(s, "Ile de France");
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
            tx.rollback();
            s.close();
            return null;
         }
      });

   }

   @Test
   public void testEntityCacheContentsAfterEvictAll() throws Exception {
      final List<Citizen> citizens = saveSomeCitizens();

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();
            Cache cache = s.getSessionFactory().getCache();

            Statistics stats = sessionFactory().getStatistics();
            SecondLevelCacheStatistics slcStats = stats.getSecondLevelCacheStatistics(Citizen.class.getName());

            assertTrue("2lc entity cache is expected to contain Citizen id = " + citizens.get(0).getId(),
                  cache.containsEntity(Citizen.class, citizens.get(0).getId()));
            assertTrue("2lc entity cache is expected to contain Citizen id = " + citizens.get(1).getId(),
                  cache.containsEntity(Citizen.class, citizens.get(1).getId()));
            assertEquals(2, slcStats.getPutCount());

            cache.evictEntityRegions();

            assertEquals(0, slcStats.getElementCountInMemory());
            assertFalse("2lc entity cache is expected to not contain Citizen id = " + citizens.get(0).getId(),
                  cache.containsEntity(Citizen.class, citizens.get(0).getId()));
            assertFalse("2lc entity cache is expected to not contain Citizen id = " + citizens.get(1).getId(),
                  cache.containsEntity(Citizen.class, citizens.get(1).getId()));

            Citizen citizen = (Citizen) s.load(Citizen.class, citizens.get(0).getId());
            assertNotNull(citizen);
            assertNotNull(citizen.getFirstname()); // proxy gets resolved
            assertEquals(1, slcStats.getMissCount());
            assertEquals(3, slcStats.getPutCount());
            assertEquals(1, slcStats.getElementCountInMemory());
            assertTrue("2lc entity cache is expected to contain Citizen id = " + citizens.get(0).getId(),
                  cache.containsEntity(Citizen.class, citizens.get(0).getId()));

            // cleanup
            tx.rollback();
            s.close();
            return null;
         }
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

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            Transaction tx = s.beginTransaction();
            s.persist( australia );
            s.persist( france );
            s.persist( c1 );
            s.persist( c2 );
            tx.commit();
            s.close();
            return null;
         }
      });

      List<Citizen> citizens = new ArrayList<Citizen>(2);
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
