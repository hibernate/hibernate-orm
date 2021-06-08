/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class DomainDataRegionNameGettersCacheDisabledTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String QUERY = "SELECT a FROM Dog a";
	private static final String REGION_NAME = "TheRegion";
	private static final String PREFIX = "test";

	@Test
	@TestForIssue( jiraKey = "HHH-13586")
	public void test() {
		rebuildSessionFactory();

		final CacheImplementor cache = sessionFactory().getCache();

		assertTrue( cache.getCacheRegionNames().isEmpty() );

		assertTrue( cache.getDomainDataRegionNames().isEmpty() );
		assertNull( cache.getDomainDataRegion( REGION_NAME ) );

		assertNull( cache.getRegion( "not a region name" ) );
		assertNull( cache.getDomainDataRegion( "not a region name" ) );
		assertNull( cache.getQueryResultsCacheStrictly( "not a region name" ) );

		// cache.getQueryCacheRegionNames() should be empty
		assertTrue( cache.getQueryCacheRegionNames().isEmpty() );
		assertNull( cache.getDefaultQueryResultsCache() );

		assertNull( cache.getQueryResultsCacheStrictly( REGION_NAME ) );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Dog.class );
	}

	@After
	public void cleanupData() {
		doInHibernate(
				this::sessionFactory, session -> {
					List<Dog> dogs = session.createQuery( "from Dog", Dog.class ).getResultList();
					for ( Dog dog : dogs ) {
						session.delete( dog );
					}
				}
		);
	}

	@Entity(name = "Dog")
	@NamedQuery(name = "Dog.findAll", query = QUERY,
			hints = {
					@QueryHint(name = "org.hibernate.cacheable", value = "true"),
					@QueryHint(name = "org.hibernate.cacheRegion", value = REGION_NAME)
			}
	)
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region= REGION_NAME)
	public static class Dog {
		@Id
		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region= REGION_NAME)
		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		public Dog(String name) {
			this.name = name;
		}

		public Dog() {
		}
	}
}
