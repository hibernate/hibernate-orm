/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for handling of data just inserted during a transaction being read from the database
 * and placed into cache.  Initially these cases went through putFromRead which causes problems because it
 * loses the context of that data having just been read.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				CacheableItem.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.CACHE_REGION_PREFIX, value = ""),
		}
)
@SessionFactory(generateStatistics = true)
public class InsertedDataTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictEntityData();
		scope.getSessionFactory().getStatistics().clear();
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		CacheableItem item = scope.fromTransaction( s -> {
			CacheableItem i = new CacheableItem( "data" );
			s.persist( i );
			return i;
		} );

		assertTrue( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertWithRollback(SessionFactoryScope scope) {
		CacheableItem item = new CacheableItem( "data" );
		try (Session s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			s.persist( item );
			s.flush();
			s.getTransaction().rollback();
		}

		assertFalse( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertThenUpdate(SessionFactoryScope scope) {
		CacheableItem item = scope.fromTransaction( s -> {
			CacheableItem i = new CacheableItem( "data" );
			s.persist( i );
			s.flush();
			i.setName( "new data" );
			return i;
		} );

		assertTrue( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertThenUpdateThenRollback(SessionFactoryScope scope) {
		CacheableItem item = new CacheableItem( "data" );
		try (Session s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			s.persist( item );
			s.flush();
			item.setName( "new data" );
			s.getTransaction().rollback();
		}

		assertFalse( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertWithRefresh(SessionFactoryScope scope) {
		CacheableItem item = scope.fromTransaction( s -> {
			CacheableItem i = new CacheableItem( "data" );
			s.persist( i );
			s.flush();
			s.refresh( i );
			return i;
		} );

		assertTrue( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertWithRefreshThenRollback(SessionFactoryScope scope) {
		CacheableItem item = new CacheableItem( "data" );
		try (Session s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			s.persist( item );
			s.flush();
			s.refresh( item );
			s.getTransaction().rollback();
		}

		assertTrue( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		CacheableItem item1 = scope.fromTransaction( s -> s.get( CacheableItem.class, item.getId() ) );

		assertNull( item1, "it should be null" );
	}

	@Test
	public void testInsertWithClear(SessionFactoryScope scope) {
		CacheableItem item = scope.fromTransaction( s -> {
			CacheableItem i = new CacheableItem( "data" );
			s.persist( i );
			s.flush();
			s.clear();
			return i;
		} );

		assertTrue( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );
	}

	@Test
	public void testInsertWithClearThenRollback(SessionFactoryScope scope) {
		CacheableItem item = new CacheableItem( "data" );
		try (Session s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			s.persist( item );
			s.flush();
			s.clear();
			item = s.get( CacheableItem.class, item.getId() );
			s.getTransaction().rollback();
		}

		assertFalse( scope.getSessionFactory().getCache().containsEntity( CacheableItem.class, item.getId() ) );

		Long id = item.getId();
		item = scope.fromTransaction( s -> s.get( CacheableItem.class, id ) );
		assertNull( item, "it should be null" );
	}
}
