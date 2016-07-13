/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.ehcache.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Zhenlei Huang
 */
@TestForIssue(jiraKey = "HHH-10649")
public class RefreshUpdatedDataTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ReadWriteCacheableItem.class,
				ReadWriteVersionedCacheableItem.class,
				NonStrictReadWriteCacheableItem.class,
				NonStrictReadWriteVersionedCacheableItem.class,
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		if ( H2Dialect.class.equals( Dialect.getDialect().getClass() ) ) {
			settings.put( Environment.URL, "jdbc:h2:mem:db-mvcc;MVCC=true" );
		}
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.configure( "hibernate-config/hibernate.cfg.xml" );
	}

	@Test
	public void testUpdateAndFlushThenRefresh() {
		// prepare data
		Session s = openSession();
		s.beginTransaction();

		final String BEFORE = "before";

		ReadWriteCacheableItem readWriteCacheableItem = new ReadWriteCacheableItem( BEFORE );
		readWriteCacheableItem.getTags().add( "Hibernate" );
		readWriteCacheableItem.getTags().add( "ORM" );
		s.persist( readWriteCacheableItem );

		ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem = new ReadWriteVersionedCacheableItem( BEFORE );
		readWriteVersionedCacheableItem.getTags().add( "Hibernate" );
		readWriteVersionedCacheableItem.getTags().add( "ORM" );
		s.persist( readWriteVersionedCacheableItem );

		NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem = new NonStrictReadWriteCacheableItem( BEFORE );
		nonStrictReadWriteCacheableItem.getTags().add( "Hibernate" );
		nonStrictReadWriteCacheableItem.getTags().add( "ORM" );
		s.persist( nonStrictReadWriteCacheableItem );

		NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem = new NonStrictReadWriteVersionedCacheableItem( BEFORE );
		nonStrictReadWriteVersionedCacheableItem.getTags().add( "Hibernate" );
		nonStrictReadWriteVersionedCacheableItem.getTags().add( "ORM" );
		s.persist( nonStrictReadWriteVersionedCacheableItem );

		s.getTransaction().commit();
		s.close();

		Session s1 = openSession();
		s1.beginTransaction();

		final String AFTER = "after";

		ReadWriteCacheableItem readWriteCacheableItem1 = s1.get( ReadWriteCacheableItem.class, readWriteCacheableItem.getId() );
		readWriteCacheableItem1.setName( AFTER );
		readWriteCacheableItem1.getTags().remove("ORM");

		ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem1 = s1.get( ReadWriteVersionedCacheableItem.class, readWriteVersionedCacheableItem.getId() );
		readWriteVersionedCacheableItem1.setName( AFTER );
		readWriteVersionedCacheableItem1.getTags().remove("ORM");

		NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem1 = s1.get( NonStrictReadWriteCacheableItem.class, nonStrictReadWriteCacheableItem.getId() );
		nonStrictReadWriteCacheableItem1.setName( AFTER );
		nonStrictReadWriteCacheableItem1.getTags().remove("ORM");

		NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem1 = s1.get( NonStrictReadWriteVersionedCacheableItem.class, nonStrictReadWriteVersionedCacheableItem.getId() );
		nonStrictReadWriteVersionedCacheableItem1.setName( AFTER );
		nonStrictReadWriteVersionedCacheableItem1.getTags().remove("ORM");

		s1.flush();
		s1.refresh( readWriteCacheableItem1 );
		s1.refresh( readWriteVersionedCacheableItem1 );
		s1.refresh( nonStrictReadWriteCacheableItem1 );
		s1.refresh( nonStrictReadWriteVersionedCacheableItem1 );

		assertEquals( AFTER, readWriteCacheableItem1.getName() );
		assertEquals( 1, readWriteCacheableItem1.getTags().size() );
		assertEquals( AFTER, readWriteVersionedCacheableItem1.getName() );
		assertEquals( 1, readWriteVersionedCacheableItem1.getTags().size() );
		assertEquals( AFTER, nonStrictReadWriteCacheableItem1.getName() );
		assertEquals( 1, nonStrictReadWriteCacheableItem1.getTags().size() );
		assertEquals( AFTER, nonStrictReadWriteVersionedCacheableItem1.getName() );
		assertEquals( 1, nonStrictReadWriteVersionedCacheableItem1.getTags().size() );

		// open another session
		Session s2 = sessionFactory().openSession();
		try {
			s2.beginTransaction();
			ReadWriteCacheableItem readWriteCacheableItem2 = s2.get( ReadWriteCacheableItem.class, readWriteCacheableItem.getId() );
			ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem2 = s2.get( ReadWriteVersionedCacheableItem.class, readWriteVersionedCacheableItem.getId() );
			NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem2 = s2.get( NonStrictReadWriteCacheableItem.class, nonStrictReadWriteCacheableItem.getId() );
			NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem2 = s2.get( NonStrictReadWriteVersionedCacheableItem.class, nonStrictReadWriteVersionedCacheableItem.getId() );

			assertEquals( BEFORE, readWriteCacheableItem2.getName() );
			assertEquals( 2, readWriteCacheableItem2.getTags().size() );
			assertEquals( BEFORE, readWriteVersionedCacheableItem2.getName() );
			assertEquals( 2, readWriteVersionedCacheableItem2.getTags().size() );

			//READ_UNCOMMITTED because there is no locking to prevent collections from being cached in the first Session

			assertEquals( BEFORE, nonStrictReadWriteCacheableItem2.getName() );
			assertEquals( 1, nonStrictReadWriteCacheableItem2.getTags().size());
			assertEquals( BEFORE, nonStrictReadWriteVersionedCacheableItem2.getName() );
			assertEquals( 1, nonStrictReadWriteVersionedCacheableItem2.getTags().size() );

			s2.getTransaction().commit();
		}
		finally {
			if ( s2.getTransaction().getStatus().canRollback() ) {
				s2.getTransaction().rollback();
			}
			s2.close();
		}

		s1.getTransaction().rollback();
		s1.close();

		s = openSession();
		s.beginTransaction();
		s.delete( readWriteCacheableItem );
		s.delete( readWriteVersionedCacheableItem );
		s.delete( nonStrictReadWriteCacheableItem );
		s.delete( nonStrictReadWriteVersionedCacheableItem );
		s.getTransaction().commit();
		s.close();
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

	@Entity(name = "NonStrictReadWriteCacheableItem")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "item")
	public static class NonStrictReadWriteCacheableItem {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
		@ElementCollection
		private List<String> tags = new ArrayList<>();

		public NonStrictReadWriteCacheableItem() {
		}

		public NonStrictReadWriteCacheableItem(String name) {
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

	@Entity(name = "NonStrictReadWriteVersionedCacheableItem")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "item")
	public static class NonStrictReadWriteVersionedCacheableItem {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
		@ElementCollection
		private List<String> tags = new ArrayList<>();

		@Version
		private int version;

		public NonStrictReadWriteVersionedCacheableItem() {
		}

		public NonStrictReadWriteVersionedCacheableItem(String name) {
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
