/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class CacheRegionStatisticsTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@JiraKey( value = "HHH-15105")
	public void testAccessDefaultQueryRegionStatistics() {
		final Statistics statistics = sessionFactory().getStatistics();
		final CacheRegionStatistics queryRegionStatistics = statistics.getQueryRegionStatistics(
				"default-query-results-region"
		);
		doInHibernate(
				this::sessionFactory, session -> {
					List<Dog> resultList = session.createQuery( "from Dog", Dog.class )
							.setCacheable( true )
							.getResultList();

					assertEquals( 1, queryRegionStatistics.getMissCount() );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		ssrb.applySetting( AvailableSettings.USE_QUERY_CACHE, true );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, new CachingRegionFactory() );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Dog.class );
	}

	@Before
	public void setupData() {
		doInHibernate(
				this::sessionFactory, session -> {
					Dog yogi = new Dog( "Yogi" );
					yogi.nickNames.add( "The Yog" );
					yogi.nickNames.add( "Little Boy" );
					yogi.nickNames.add( "Yogaroni Macaroni" );
					Dog irma = new Dog( "Irma" );
					irma.nickNames.add( "Squirmy" );
					irma.nickNames.add( "Bird" );
					session.persist( yogi );
					session.persist( irma );
				}
		);
	}

	@After
	public void cleanupData() {
		doInHibernate(
				this::sessionFactory, session -> {
					List<Dog> dogs = session.createQuery( "from Dog", Dog.class ).getResultList();
					for ( Dog dog : dogs ) {
						session.remove( dog );
					}
				}
		);
	}

	@Entity(name = "Dog")
	public static class Dog {
		@Id
		private String name;

		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		public Dog(String name) {
			this.name = name;
		}

		public Dog() {
		}
	}
}
