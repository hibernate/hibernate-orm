/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for handling of data just inserted during a transaction being read from the database
 * and placed into cache.  Initially these cases went through putFromRead which causes problems because it
 * loses the context of that data having just been read.
 *
 * @author Steve Ebersole
 */
public class InsertedDataTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CacheableItem.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, true );
	}

	@Test
	public void testInsert() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.getTransaction().commit();
		s.close();

		assertTrue( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRollback() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		s.getTransaction().rollback();
		s.close();

		assertFalse( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertThenUpdate() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().commit();
		s.close();

		assertTrue( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );


		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertThenUpdateThenRollback() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		item.setName( "new data" );
		s.getTransaction().rollback();
		s.close();

		assertFalse( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRefresh() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().commit();
		s.close();

		assertTrue( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithRefreshThenRollback() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		s.refresh( item );
		s.getTransaction().rollback();
		s.close();

		assertTrue( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
//		Object lock = cacheMap.values().iterator().next();
//		assertEquals( "org.hibernate.testing.cache.AbstractReadWriteAccessStrategy$Lock", lock.getClass().getName() );

		s = openSession();
		s.beginTransaction();
		item = s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}

	@Test
	public void testInsertWithClear() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		s.clear();
		s.getTransaction().commit();
		s.close();

		assertTrue( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInsertWithClearThenRollback() {
		sessionFactory().getCache().evictEntityData();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.persist( item );
		s.flush();
		s.clear();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().rollback();
		s.close();

		assertFalse( sessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		s = openSession();
		s.beginTransaction();
		item = (CacheableItem) s.get( CacheableItem.class, item.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "it should be null", item );
	}
}
