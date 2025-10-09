/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.caching;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class CachingWithSecondaryTablesTests {
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
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		assertFalse( sessionFactory.getCache().contains( Person.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final Person person = s.find( Person.class, "1" );
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
		assertEquals( 1, statistics.getSecondLevelCachePutCount() );
		assertTrue( sessionFactory.getCache().contains( Person.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final Person person = s.find( Person.class, "1" );
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
		assertEquals( 1, statistics.getSecondLevelCachePutCount() );
		assertTrue( sessionFactory.getCache().contains( VersionedPerson.class, "1" ) );

		inTransaction(
				sessionFactory,
				s -> {
					statistics.clear();

					final VersionedPerson person = s.find( VersionedPerson.class, "1" );
					assertTrue( Hibernate.isInitialized( person ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is( 1L ) );

					statistics.clear();
				}
		);
	}


	private SessionFactoryImplementor buildSessionFactory(Class<?> entityClass, boolean strict) {
		final Map<String,Object> settings = new HashMap<>();
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		if ( strict ) {
			settings.put( AvailableSettings.JPA_CACHING_COMPLIANCE, "true" );
		}

		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				.build();
		try {
			return (SessionFactoryImplementor) new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Person.class )
					.addAnnotatedClass( VersionedPerson.class )
					.buildMetadata()
					.buildSessionFactory();
		}
		catch (Throwable t) {
			serviceRegistry.close();
			throw t;
		}
	}

	@AfterEach
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
