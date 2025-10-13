/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Zhenlei Huang
 */
@JiraKey(value = "HHH-10649")
@RequiresDialect(value = H2Dialect.class)
@DomainModel(
		annotatedClasses = {
				RefreshUpdatedDataTest.ReadWriteCacheableItem.class,
				RefreshUpdatedDataTest.ReadWriteVersionedCacheableItem.class,
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.URL,
						value = "jdbc:h2:mem:db-mvcc;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.CACHE_REGION_PREFIX, value = ""),
				@Setting(name = Environment.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory"),
		}
)
@SessionFactory(generateStatistics = true)
public class RefreshUpdatedDataTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testUpdateAndFlushThenRefresh(SessionFactoryScope scope) {
		// prepare data
		ReadWriteCacheableItem readWriteCacheableItem;
		ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem;
		final String BEFORE = "before";
		final String AFTER = "after";

		try (var s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			readWriteCacheableItem = new ReadWriteCacheableItem( BEFORE );
			readWriteCacheableItem.getTags().add( "Hibernate" );
			readWriteCacheableItem.getTags().add( "ORM" );
			s.persist( readWriteCacheableItem );

			readWriteVersionedCacheableItem = new ReadWriteVersionedCacheableItem( BEFORE );
			readWriteVersionedCacheableItem.getTags().add( "Hibernate" );
			readWriteVersionedCacheableItem.getTags().add( "ORM" );
			s.persist( readWriteVersionedCacheableItem );
			s.getTransaction().commit();
		}

		try (var s1 = scope.getSessionFactory().openSession()) {
			s1.beginTransaction();
			ReadWriteCacheableItem readWriteCacheableItem1 = s1.find( ReadWriteCacheableItem.class,
					readWriteCacheableItem.getId() );
			readWriteCacheableItem1.setName( AFTER );
			readWriteCacheableItem1.getTags().remove( "ORM" );

			ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem1 = s1.find(
					ReadWriteVersionedCacheableItem.class, readWriteVersionedCacheableItem.getId() );
			readWriteVersionedCacheableItem1.setName( AFTER );
			readWriteVersionedCacheableItem1.getTags().remove( "ORM" );

			s1.flush();
			s1.refresh( readWriteCacheableItem1 );
			s1.refresh( readWriteVersionedCacheableItem1 );

			assertEquals( AFTER, readWriteCacheableItem1.getName() );
			assertEquals( 1, readWriteCacheableItem1.getTags().size() );
			assertEquals( AFTER, readWriteVersionedCacheableItem1.getName() );
			assertEquals( 1, readWriteVersionedCacheableItem1.getTags().size() );

			// open another session
			try (Session s2 = scope.getSessionFactory().openSession()) {
				s2.beginTransaction();
				ReadWriteCacheableItem readWriteCacheableItem2 = s2.find( ReadWriteCacheableItem.class,
						readWriteCacheableItem.getId() );
				ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem2 = s2.find(
						ReadWriteVersionedCacheableItem.class, readWriteVersionedCacheableItem.getId() );

				assertEquals( BEFORE, readWriteCacheableItem2.getName() );
				assertEquals( 2, readWriteCacheableItem2.getTags().size() );
				assertEquals( BEFORE, readWriteVersionedCacheableItem2.getName() );
				assertEquals( 2, readWriteVersionedCacheableItem2.getTags().size() );

				s2.getTransaction().commit();
			}

			s1.getTransaction().rollback();
		}
	}

	@Test
	public void testExternalUpdateRefresh(SessionFactoryScope scope) {
		// prepare data
		final String BEFORE = "before";
		ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem = new ReadWriteVersionedCacheableItem( BEFORE );
		try (var s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();
			readWriteVersionedCacheableItem.getTags().add( "Hibernate" );
			readWriteVersionedCacheableItem.getTags().add( "ORM" );
			s.persist( readWriteVersionedCacheableItem );

			s.getTransaction().commit();
		}

		Session s2 = null;
		int v = 0;
		try (var s = scope.getSessionFactory().openSession()) {
			s.beginTransaction();

			// Read the entry in to populate 2LC
			readWriteVersionedCacheableItem = s.find( ReadWriteVersionedCacheableItem.class,
					readWriteVersionedCacheableItem.getId() );

			// open another session
			s2 = scope.getSessionFactory().openSession();
			s2.beginTransaction();
			// Read from 2LC
			readWriteVersionedCacheableItem = s2.find( ReadWriteVersionedCacheableItem.class,
					readWriteVersionedCacheableItem.getId() );

			assertEquals( BEFORE, readWriteVersionedCacheableItem.getName() );
			assertEquals( 2, readWriteVersionedCacheableItem.getTags().size() );

			// Change the value externally
			int version = readWriteVersionedCacheableItem.version + 1;
			v = version;
			long id = readWriteVersionedCacheableItem.id;

			s.doWork( connection -> {
				try (Statement stmt = connection.createStatement()) {
					stmt.executeUpdate(
							"UPDATE ReadWriteVersionedCacheableItem SET version = " + version + " WHERE id = " + id );
				}
			} );

			s.getTransaction().commit();

		}
		if ( s2 == null ) {
			fail();
		}
		s2.refresh( readWriteVersionedCacheableItem );

		assertEquals( v, readWriteVersionedCacheableItem.version );
		assertEquals( 2, readWriteVersionedCacheableItem.getTags().size() );

		s2.remove( readWriteVersionedCacheableItem );
		s2.getTransaction().commit();
		s2.close();
	}

	@Entity(name = "ReadWriteCacheableItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "item")
	public static class ReadWriteCacheableItem {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@ElementCollection
		private List<String> tags = new ArrayList<>();

		public ReadWriteCacheableItem() {
		}

		public ReadWriteCacheableItem(String name) {
			this.name = name;
		}

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

		public List<String> getTags() {
			return tags;
		}
	}

	@Entity(name = "ReadWriteVersionedCacheableItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "item")
	public static class ReadWriteVersionedCacheableItem {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@ElementCollection
		private List<String> tags = new ArrayList<>();

		@Version
		private int version;

		public ReadWriteVersionedCacheableItem() {
		}

		public ReadWriteVersionedCacheableItem(String name) {
			this.name = name;
		}

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

		public List<String> getTags() {
			return tags;
		}
	}
}
