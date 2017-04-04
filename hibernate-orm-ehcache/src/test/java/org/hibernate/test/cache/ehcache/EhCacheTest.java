/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.ehcache;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Alex Snaps
 */
public abstract class EhCacheTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	public String getBaseForMappings() {
		return "org/hibernate/test/cache/ehcache/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "Item.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "read-write";
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( Environment.CACHE_REGION_PREFIX, "" );
		settings.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.USE_STRUCTURED_CACHE, "true" );
//		settings.put( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class.getName() );
	}

	@Test
	public void testQueryCacheInvalidation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName( "widget" );
		i.setDescription( "A really top-quality, full-featured widget." );
		s.persist( i );
		t.commit();
		s.close();

		SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics()
				.getSecondLevelCacheStatistics( Item.class.getName() );

		assertEquals( slcs.getPutCount(), 1 );
		assertEquals( slcs.getElementCountInMemory(), 1 );
		assertEquals( slcs.getEntries().size(), 1 );

		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.get( Item.class, i.getId() );

		assertEquals( slcs.getHitCount(), 1 );
		assertEquals( slcs.getMissCount(), 0 );

		i.setDescription( "A bog standard item" );

		t.commit();
		s.close();

		assertEquals( slcs.getPutCount(), 2 );

		Object entry = slcs.getEntries().get( i.getId() );
		Map map;
		map = getMapFromCacheEntry( entry );
		assertTrue( map.get( "description" ).equals( "A bog standard item" ) );
		assertTrue( map.get( "name" ).equals( "widget" ) );

		// cleanup
		s = openSession();
		t = s.beginTransaction();
		s.delete( i );
		t.commit();
		s.close();
	}

	protected abstract Map getMapFromCacheEntry(final Object entry);

	@Test
	public void testEmptySecondLevelCacheEntry() throws Exception {
		sessionFactory().getCache().evictEntityRegion( Item.class.getName() );
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics( Item.class.getName() );
		Map cacheEntries = statistics.getEntries();
		assertEquals( 0, cacheEntries.size() );
	}

	@SuppressWarnings( { "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment" })
	@Test
	public void testStaleWritesLeaveCacheConsistent() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		VersionedItem item = new VersionedItem();
		item.setName( "steve" );
		item.setDescription( "steve's item" );
		s.save( item );
		txn.commit();
		s.close();

		Long initialVersion = item.getVersion();

		// manually revert the version property
		item.setVersion( Long.valueOf( item.getVersion().longValue() - 1 ) );

		try {
			s = openSession();
			txn = s.beginTransaction();
			s.update( item );
			txn.commit();
			s.close();
			fail( "expected stale write to fail" );
		}
		catch ( Throwable expected ) {
			// expected behavior here
			if ( txn != null ) {
				try {
					txn.rollback();
				}
				catch ( Throwable ignore ) {
				}
			}
		}
		finally {
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch ( Throwable ignore ) {
				}
			}
		}

		// check the version value in the cache...
		SecondLevelCacheStatistics slcs = sessionFactory().getStatistics()
				.getSecondLevelCacheStatistics( VersionedItem.class.getName() );

		Object entry = slcs.getEntries().get( item.getId() );
		Long cachedVersionValue;
//		if ( entry instanceof ReadWriteCache.Lock ) {
//			//FIXME don't know what to test here
//			cachedVersionValue = Long.valueOf( ((ReadWriteCache.Lock) entry).getUnlockTimestamp() );
//		} else
		if ( entry.getClass()
				.getName()
				.equals( "org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Lock" ) ) {
			//FIXME don't know what to test here
		}
		else {
			cachedVersionValue = (Long) getMapFromCacheEntry( entry ).get( "_version" );
			assertEquals( initialVersion.longValue(), cachedVersionValue.longValue() );
		}


		// cleanup
		s = openSession();
		txn = s.beginTransaction();
		item = (VersionedItem) s.load( VersionedItem.class, item.getId() );
		s.delete( item );
		txn.commit();
		s.close();

	}

}
