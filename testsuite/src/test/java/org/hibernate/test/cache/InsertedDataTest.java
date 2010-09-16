/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.annotations.TestCase;

/**
 * Tests for handling of data just inserted during a transaction being read from the database
 * and placed into cache.  Initially these cases went through putFromRead which causes problems because it
 * loses the context of that data having just been read.
 *
 * @author Steve Ebersole
 */
public class InsertedDataTest extends TestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CacheableItem.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public void testInsert() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertWithRollback() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );
	}

	public void testInsertThenUpdate() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertThenUpdateThenRollback() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertWithRefresh() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertWithRefreshThenRollback() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}

	public void testInsertWithClear() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.clear();
		s.getTransaction().commit();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertWithClearThenRollback() {
		getSessions().getCache().evictEntityRegions();
		getSessions().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.clear();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = getSessions().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}
}
