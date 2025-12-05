/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = SecondLevelCacheTest.Person.class,
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "jcache"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true")
		}
)
public class SecondLevelCacheTest {
	private final Logger log = Logger.getLogger( SecondLevelCacheTest.class );

	@Test
	public void testCache(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist(new Person());
			Person aPerson= new Person();
			aPerson.setName("John Doe");
			aPerson.setCode("unique-code");
			entityManager.persist(aPerson);
		});

		scope.inTransaction( entityManager -> {
			log.info("Jpa load by id");
			//tag::caching-entity-jpa-example[]
			Person person = entityManager.find(Person.class, 1L);
			//end::caching-entity-jpa-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Native load by id");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-entity-native-example[]
			Person person = session.find(Person.class, 1L);
			//end::caching-entity-native-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Native load by natural-id");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-entity-natural-id-example[]
			Person person = session
				.byNaturalId(Person.class)
				.using("code", "unique-code")
				.load();
			//end::caching-entity-natural-id-example[]
			assertNotNull(person);
		});

		scope.inTransaction( entityManager -> {
			log.info("Jpa query cache");
			//tag::caching-query-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name = :name", Person.class)
			.setParameter("name", "John Doe")
			.setHint("org.hibernate.cacheable", "true")
			.getResultList();
			//end::caching-query-jpa-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Native query cache");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-query-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name = :name", Person.class)
			.setParameter("name", "John Doe")
			.setCacheable(true)
			.list();
			//end::caching-query-native-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Jpa query cache region");
			//tag::caching-query-region-jpa-example[]
			List<Person> persons = entityManager.createQuery(
					"select p " +
					"from Person p " +
					"where p.id > :id", Person.class)
					.setParameter("id", 0L)
					.setHint(HINT_CACHEABLE, "true")
					.setHint(HINT_CACHE_REGION, "query.cache.person")
					.getResultList();
			//end::caching-query-region-jpa-example[]
		});
		scope.inTransaction( entityManager -> {
			log.info("Native query cache");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-query-region-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id", Person.class)
			.setParameter("id", 0L)
			.setCacheable(true)
			.setCacheRegion("query.cache.person")
			.list();
			//end::caching-query-region-native-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Jpa query cache store mode ");
			//tag::caching-query-region-store-mode-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id", Person.class)
			.setParameter("id", 0L)
			.setHint(HINT_CACHEABLE, "true")
			.setHint(HINT_CACHE_REGION, "query.cache.person")
			.setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
			.getResultList();
			//end::caching-query-region-store-mode-jpa-example[]
		});
		scope.inTransaction( entityManager -> {
			log.info("Native query cache store mode");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-query-region-store-mode-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id", Person.class)
			.setParameter("id", 0L)
			.setCacheable(true)
			.setCacheRegion("query.cache.person")
			.setCacheMode(CacheMode.REFRESH)
			.list();
			//end::caching-query-region-store-mode-native-example[]
		});

		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-statistics-example[]
			Statistics statistics = session.getSessionFactory().getStatistics();
			CacheRegionStatistics secondLevelCacheStatistics =
					statistics.getDomainDataRegionStatistics("query.cache.person");
			long hitCount = secondLevelCacheStatistics.getHitCount();
			long missCount = secondLevelCacheStatistics.getMissCount();
			double hitRatio = (double) hitCount / (hitCount + missCount);
			//end::caching-statistics-example[]
		});

		scope.inTransaction( entityManager -> {
			log.info("Native query cache store mode");
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-query-region-native-evict-example[]
			session.getSessionFactory().getCache().evictQueryRegion("query.cache.person");
			//end::caching-query-region-native-evict-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::caching-management-cache-mode-entity-jpa-example[]
			Map<String, Object> hints = new HashMap<>();
			hints.put("jakarta.persistence.cache.retrieveMode" , CacheRetrieveMode.USE);
			hints.put("jakarta.persistence.cache.storeMode" , CacheStoreMode.REFRESH);
			Person person = entityManager.find(Person.class, 1L , hints);
			//end::caching-management-cache-mode-entity-jpa-example[]
		});

		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-management-cache-mode-entity-native-example[]
			session.setCacheMode(CacheMode.REFRESH);
			Person person = session.find(Person.class, 1L);
			//end::caching-management-cache-mode-entity-native-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::caching-management-cache-mode-query-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p from Person p", Person.class)
			.setHint(HINT_CACHEABLE, "true")
			.setHint("jakarta.persistence.cache.retrieveMode" , CacheRetrieveMode.USE)
			.setHint("jakarta.persistence.cache.storeMode" , CacheStoreMode.REFRESH)
			.getResultList();
			//end::caching-management-cache-mode-query-jpa-example[]

			//tag::caching-management-evict-jpa-example[]
			entityManager.getEntityManagerFactory().getCache().evict(Person.class);
			//end::caching-management-evict-jpa-example[]
		});

		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::caching-management-cache-mode-query-native-example[]
			List<Person> persons = session.createQuery(
				"select p from Person p", Person.class)
			.setCacheable(true)
			.setCacheMode(CacheMode.REFRESH)
			.list();
			//end::caching-management-cache-mode-query-native-example[]

			//tag::caching-management-evict-native-example[]
			session.getSessionFactory().getCache().evictQueryRegion("query.cache.person");
			//end::caching-management-evict-native-example[]
		});
	}

	//tag::caching-entity-natural-id-mapping-example[]
	@Entity(name = "Person")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		@NaturalId
		@Column(name = "code", unique = true)
		private String code;

		//Getters and setters are omitted for brevity

	//end::caching-entity-natural-id-mapping-example[]

		public Person() {}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	//tag::caching-entity-natural-id-mapping-example[]
	}
	//end::caching-entity-natural-id-mapping-example[]
}
