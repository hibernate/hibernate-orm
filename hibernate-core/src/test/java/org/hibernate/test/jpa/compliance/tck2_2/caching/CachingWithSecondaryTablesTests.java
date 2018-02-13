/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2.caching;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class CachingWithSecondaryTablesTests extends BaseUnitTestCase {
	private SessionFactoryImplementor sessionFactory;

	@Test
	public void testUnstrictUnversioned() {
		sessionFactory = buildSessionFactory( Person.class, false );

		final StatisticsImplementor statistics = sessionFactory.getStatistics();

		inTransaction(
				sessionFactory,
				s -> s.persist( new Person( "1", "John Doe", true ) )
		);

		// it should not be in the cache because it should be invalidated instead
		assertEquals( statistics.getSecondLevelCachePutCount(), 0 );
		assertFalse( sessionFactory.getCache().contains( Person.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final Person person = s.get( Person.class, "1" );
					assertTrue( Hibernate.isInitialized( person ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is( 0L) );

					statistics.clear();
				}
		);
	}

	@Test
	public void testStrictUnversioned() {
		sessionFactory = buildSessionFactory( Person.class, true );

		final StatisticsImplementor statistics = sessionFactory.getStatistics();

		inTransaction(
				sessionFactory,
				s -> s.persist( new Person( "1", "John Doe", true ) )
		);

		// this time it should be iun the cache because we enabled JPA compliance
		assertEquals( statistics.getSecondLevelCachePutCount(), 1 );
		assertTrue( sessionFactory.getCache().contains( Person.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final Person person = s.get( Person.class, "1" );
					assertTrue( Hibernate.isInitialized( person ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is( 1L) );

					statistics.clear();
				}
		);
	}

	@Test
	public void testVersioned() {
		sessionFactory = buildSessionFactory( VersionedPerson.class, false );

		final StatisticsImplementor statistics = sessionFactory.getStatistics();

		inTransaction(
				sessionFactory,
				s -> s.persist( new VersionedPerson( "1", "John Doe", true ) )
		);

		// versioned data should be cacheable regardless
		assertEquals( statistics.getSecondLevelCachePutCount(), 1 );
		assertTrue( sessionFactory.getCache().contains( VersionedPerson.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final VersionedPerson person = s.get( VersionedPerson.class, "1" );
					assertTrue( Hibernate.isInitialized( person ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is( 1L ) );

					statistics.clear();
				}
		);
	}


	private SessionFactoryImplementor buildSessionFactory(Class entityClass, boolean strict) {
		final Map settings = new HashMap();
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		if ( strict ) {
			settings.put( AvailableSettings.JPA_CACHING_COMPLIANCE, "true" );
		}

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
				.applySettings( settings );

		return (SessionFactoryImplementor) new MetadataSources( ssrb.build() )
				.addAnnotatedClass( Person.class )
				.addAnnotatedClass( VersionedPerson.class)
				.buildMetadata()
				.buildSessionFactory();

	}

	@After
	public void cleanupData() {
		if ( sessionFactory == null ) {
			return;
		}
		inTransaction(
				sessionFactory,
				s -> {
					s.createQuery( "delete from Person" ).executeUpdate();
				}
		);
		sessionFactory.close();
	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Cacheable()
	@SecondaryTable( name = "crm_persons" )
	public static class Person {
		@Id
		public String id;
		public String name;

		@Column( table = "crm_persons" )
		public boolean crmMarketingSchpele;

		public Person() {
		}

		public Person(String id, String name, boolean crmMarketingSchpele) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "VersionedPerson" )
	@Table( name = "versioned_persons" )
	@Cacheable()
	@SecondaryTable( name = "crm_persons2" )
	public static class VersionedPerson {
		@Id
		public String id;
		public String name;

		@Version public int version;

		@Column( table = "crm_persons2" )
		public boolean crmMarketingSchpele;

		public VersionedPerson() {
		}

		public VersionedPerson(String id, String name, boolean crmMarketingSchpele) {
			this.id = id;
			this.name = name;
		}
	}
}
