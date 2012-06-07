/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache.ehcache;

import java.util.Map;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Alex Snaps
 */
public abstract class EhCacheTest extends BaseCoreFunctionalTestCase {
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
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_STRUCTURED_CACHE, "true" );
		configCache( cfg );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class.getName() );
	}

	protected abstract void configCache(final Configuration cfg);

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
