/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.ehcache.functional;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for handling of data just inserted during a transaction being read from the database
 * and placed into cache.  Initially these cases went through putFromRead which causes problems because it
 * loses the context of that data having just been read.
 *
 * @author Steve Ebersole
 */
public class InsertedDataTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CacheableItem.class};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, "" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.configure( "hibernate-config/hibernate.cfg.xml" );
	}

	@Test
	public void testInsert() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRollback() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );
	}

	@Test
	public void testInsertThenUpdate() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertThenUpdateThenRollback() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRefresh() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().commit();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRefreshThenRollback() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );
		Object lock = cacheMap.values().iterator().next();
		assertEquals( "org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Lock", lock.getClass().getName() );

		s = openSession();
		s.beginTransaction();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}

	@Test
	public void testInsertWithClear() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.clear();
		s.getTransaction().commit();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 1, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithClearThenRollback() {
		sessionFactory().getCache().evictEntityRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.flush();
		s.clear();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().rollback();
		s.close();

		Map cacheMap = sessionFactory().getStatistics().getSecondLevelCacheStatistics( "item" ).getEntries();
		assertEquals( 0, cacheMap.size() );

		s = openSession();
		s.beginTransaction();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}

	@Entity(name = "CacheableItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "item")
	public static class CacheableItem {
		private Long id;
		private String name;

		public CacheableItem() {
		}

		public CacheableItem(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
