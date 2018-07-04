/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jcache.test.TestHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;

/**
 * @author Zhenlei Huang
 */
@TestForIssue(jiraKey = "HHH-10649")
public class RefreshUpdatedDataTest extends BaseUnitTestCase {
	private ServiceRegistry serviceRegistry;
	private SessionFactoryImplementor sessionFactory;

	@Before
	@SuppressWarnings("unused")
	public void acquireResources() {
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
				.configure( "hibernate-config/hibernate.cfg.xml" );

		if ( H2Dialect.class.equals( Dialect.getDialect().getClass() ) ) {
			ssrb.applySetting( AvailableSettings.URL, "jdbc:h2:mem:db-mvcc;MVCC=true" );
		}
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );


		serviceRegistry = ssrb
				.configure( "hibernate-config/hibernate.cfg.xml" )
				.applySetting( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE )
				.build();

		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addAnnotatedClass( ReadWriteCacheableItem.class );
		metadataSources.addAnnotatedClass( ReadWriteVersionedCacheableItem.class );
		metadataSources.addAnnotatedClass( NonStrictReadWriteCacheableItem.class );
		metadataSources.addAnnotatedClass( NonStrictReadWriteVersionedCacheableItem.class );

		final Metadata metadata = metadataSources.buildMetadata();
		TestHelper.createRegions( metadata, true );

		sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
	}

	@After
	@SuppressWarnings("unused")
	public void releaseResources() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}

		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testUpdateAndFlushThenRefresh() {
		final String BEFORE = "before";

		inTransaction(
				sessionFactory,
				session -> {

					ReadWriteCacheableItem readWriteCacheableItem = new ReadWriteCacheableItem( BEFORE );
					readWriteCacheableItem.getTags().add( "Hibernate" );
					readWriteCacheableItem.getTags().add( "ORM" );
					session.persist( readWriteCacheableItem );

					ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem = new ReadWriteVersionedCacheableItem( BEFORE );
					readWriteVersionedCacheableItem.getTags().add( "Hibernate" );
					readWriteVersionedCacheableItem.getTags().add( "ORM" );
					session.persist( readWriteVersionedCacheableItem );

					NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem = new NonStrictReadWriteCacheableItem( BEFORE );
					nonStrictReadWriteCacheableItem.getTags().add( "Hibernate" );
					nonStrictReadWriteCacheableItem.getTags().add( "ORM" );
					session.persist( nonStrictReadWriteCacheableItem );

					NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem = new NonStrictReadWriteVersionedCacheableItem( BEFORE );
					nonStrictReadWriteVersionedCacheableItem.getTags().add( "Hibernate" );
					nonStrictReadWriteVersionedCacheableItem.getTags().add( "ORM" );
					session.persist( nonStrictReadWriteVersionedCacheableItem );
				}
		);

		inTransaction(
				sessionFactory,
				s1 -> {
					final String AFTER = "after";

					ReadWriteCacheableItem readWriteCacheableItem1 = s1.get( ReadWriteCacheableItem.class, 1L );
					readWriteCacheableItem1.setName( AFTER );
					readWriteCacheableItem1.getTags().remove("ORM");

					ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem1 = s1.get( ReadWriteVersionedCacheableItem.class, 1L );
					readWriteVersionedCacheableItem1.setName( AFTER );
					readWriteVersionedCacheableItem1.getTags().remove("ORM");

					NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem1 = s1.get( NonStrictReadWriteCacheableItem.class, 1L );
					nonStrictReadWriteCacheableItem1.setName( AFTER );
					nonStrictReadWriteCacheableItem1.getTags().remove("ORM");

					NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem1 = s1.get( NonStrictReadWriteVersionedCacheableItem.class, 1L );
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

					inTransaction(
							sessionFactory,
							s2 -> {
								ReadWriteCacheableItem readWriteCacheableItem2 = s2.get( ReadWriteCacheableItem.class, 1L );
								ReadWriteVersionedCacheableItem readWriteVersionedCacheableItem2 = s2.get( ReadWriteVersionedCacheableItem.class, 1L );
								NonStrictReadWriteCacheableItem nonStrictReadWriteCacheableItem2 = s2.get( NonStrictReadWriteCacheableItem.class, 1L );
								NonStrictReadWriteVersionedCacheableItem nonStrictReadWriteVersionedCacheableItem2 = s2.get( NonStrictReadWriteVersionedCacheableItem.class, 1L );

								assertEquals( BEFORE, readWriteCacheableItem2.getName() );
								assertEquals( 2, readWriteCacheableItem2.getTags().size() );
								assertEquals( BEFORE, readWriteVersionedCacheableItem2.getName() );
								assertEquals( 2, readWriteVersionedCacheableItem2.getTags().size() );

								// AFTER because there is no locking to block the put from the second session

								assertEquals( AFTER, nonStrictReadWriteCacheableItem2.getName() );
								assertEquals( 1, nonStrictReadWriteCacheableItem2.getTags().size());
								assertEquals( AFTER, nonStrictReadWriteVersionedCacheableItem2.getName() );
								assertEquals( 1, nonStrictReadWriteVersionedCacheableItem2.getTags().size() );
							}
					);
				}
		);

		inTransaction(
				sessionFactory,
				s -> {
					s.delete( s.getReference( ReadWriteCacheableItem.class, 1L ) );
					s.delete( s.getReference( ReadWriteVersionedCacheableItem.class, 1L ) );
					s.delete( s.getReference( NonStrictReadWriteCacheableItem.class, 1L ) );
					s.delete( s.getReference( NonStrictReadWriteVersionedCacheableItem.class, 1L ) );
				}
		);
	}

	@Entity(name = "RwItem")
	@Table(name = "RW_ITEM")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "item")
	public static class ReadWriteCacheableItem {

		@Id
		@GeneratedValue(generator = "increment")
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

	@Entity(name = "RwVersionedItem")
	@Table(name = "RW_VERSIONED_ITEM")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "item")
	public static class ReadWriteVersionedCacheableItem {

		@Id
		@GeneratedValue(generator = "increment")
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

	@Entity(name = "NrwItem")
	@Table(name = "RW_NOSTRICT_ITEM")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "item")
	public static class NonStrictReadWriteCacheableItem {

		@Id
		@GeneratedValue(generator = "increment")
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

	@Entity(name = "NrwVersionedItem")
	@Table(name = "RW_NOSTRICT_VER_ITEM")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "item")
	public static class NonStrictReadWriteVersionedCacheableItem {

		@Id
		@GeneratedValue(generator = "increment")
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
