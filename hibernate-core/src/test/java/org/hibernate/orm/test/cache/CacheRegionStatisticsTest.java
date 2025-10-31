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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				CacheRegionStatisticsTest.Dog.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false"),
				@Setting(name = AvailableSettings.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory"),
		}
)
public class CacheRegionStatisticsTest {

	@Test
	@JiraKey(value = "HHH-15105")
	public void testAccessDefaultQueryRegionStatistics(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();
		final CacheRegionStatistics queryRegionStatistics = statistics.getQueryRegionStatistics(
				"default-query-results-region"
		);
		scope.inTransaction( session -> {
					List<Dog> resultList = session.createQuery( "from Dog", Dog.class )
							.setCacheable( true )
							.getResultList();

					assertEquals( 1, queryRegionStatistics.getMissCount() );
				}
		);
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
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
