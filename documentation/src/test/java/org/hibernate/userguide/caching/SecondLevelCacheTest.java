/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.caching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import org.junit.Ignore;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@Ignore
//@FailureExpected( jiraKey = "HHH-12146", message = "No idea why those changes cause this to fail, especially in the way it does" )
public class SecondLevelCacheTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE.toString() );
        options.put( AvailableSettings.CACHE_REGION_FACTORY, "jcache" );
        options.put( AvailableSettings.USE_QUERY_CACHE, Boolean.TRUE.toString() );
        options.put( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString() );
        //options.put( AvailableSettings.CACHE_REGION_PREFIX, "" );
    }

    @Test
    public void testCache() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            entityManager.persist( new Person() );
			Person aPerson= new Person();
			aPerson.setName( "John Doe" );
			aPerson.setCode( "unique-code" );
            entityManager.persist( aPerson );
			return aPerson;
        });

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Jpa load by id" );
			//tag::caching-entity-jpa-example[]
			Person person = entityManager.find( Person.class, 1L );
			//end::caching-entity-jpa-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native load by id" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-entity-native-example[]
			Person person = session.get( Person.class, 1L );
			//end::caching-entity-native-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native load by natural-id" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-entity-natural-id-example[]
			Person person = session
				.byNaturalId( Person.class )
				.using( "code", "unique-code")
				.load();
			//end::caching-entity-natural-id-example[]
			assertNotNull(person);
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Jpa query cache" );
			//tag::caching-query-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name = :name", Person.class)
			.setParameter( "name", "John Doe")
			.setHint( "org.hibernate.cacheable", "true")
			.getResultList();
			//end::caching-query-jpa-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native query cache" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-query-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name = :name")
			.setParameter( "name", "John Doe")
			.setCacheable(true)
			.list();
			//end::caching-query-native-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Jpa query cache region" );
			//tag::caching-query-region-jpa-example[]
			List<Person> persons = entityManager.createQuery(
					"select p " +
					"from Person p " +
					"where p.id > :id", Person.class)
					.setParameter( "id", 0L)
					.setHint( QueryHints.HINT_CACHEABLE, "true")
					.setHint( QueryHints.HINT_CACHE_REGION, "query.cache.person" )
					.getResultList();
			//end::caching-query-region-jpa-example[]
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native query cache" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-query-region-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id")
			.setParameter( "id", 0L)
			.setCacheable(true)
			.setCacheRegion( "query.cache.person" )
			.list();
			//end::caching-query-region-native-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Jpa query cache store mode " );
			//tag::caching-query-region-store-mode-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id", Person.class)
			.setParameter( "id", 0L)
			.setHint( QueryHints.HINT_CACHEABLE, "true")
			.setHint( QueryHints.HINT_CACHE_REGION, "query.cache.person" )
			.setHint( "javax.persistence.cache.storeMode", CacheStoreMode.REFRESH )
			.getResultList();
			//end::caching-query-region-store-mode-jpa-example[]
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native query cache store mode" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-query-region-store-mode-native-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.id > :id")
			.setParameter( "id", 0L)
			.setCacheable(true)
			.setCacheRegion( "query.cache.person" )
			.setCacheMode( CacheMode.REFRESH )
			.list();
			//end::caching-query-region-store-mode-native-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-statistics-example[]
			Statistics statistics = session.getSessionFactory().getStatistics();
			CacheRegionStatistics secondLevelCacheStatistics =
					statistics.getDomainDataRegionStatistics( "query.cache.person" );
			long hitCount = secondLevelCacheStatistics.getHitCount();
			long missCount = secondLevelCacheStatistics.getMissCount();
			double hitRatio = (double) hitCount / ( hitCount + missCount );
			//end::caching-statistics-example[]
			return hitRatio;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Native query cache store mode" );
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-query-region-native-evict-example[]
			session.getSessionFactory().getCache().evictQueryRegion( "query.cache.person" );
			//end::caching-query-region-native-evict-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::caching-management-cache-mode-entity-jpa-example[]
			Map<String, Object> hints = new HashMap<>(  );
			hints.put( "javax.persistence.cache.retrieveMode " , CacheRetrieveMode.USE );
			hints.put( "javax.persistence.cache.storeMode" , CacheStoreMode.REFRESH );
			Person person = entityManager.find( Person.class, 1L , hints);
			//end::caching-management-cache-mode-entity-jpa-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-management-cache-mode-entity-native-example[]
			session.setCacheMode( CacheMode.REFRESH );
			Person person = session.get( Person.class, 1L );
			//end::caching-management-cache-mode-entity-native-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::caching-management-cache-mode-query-jpa-example[]
			List<Person> persons = entityManager.createQuery(
				"select p from Person p", Person.class)
			.setHint( QueryHints.HINT_CACHEABLE, "true")
			.setHint( "javax.persistence.cache.retrieveMode " , CacheRetrieveMode.USE )
			.setHint( "javax.persistence.cache.storeMode" , CacheStoreMode.REFRESH )
			.getResultList();
			//end::caching-management-cache-mode-query-jpa-example[]

			//tag::caching-management-evict-jpa-example[]
			entityManager.getEntityManagerFactory().getCache().evict( Person.class );
			//end::caching-management-evict-jpa-example[]
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::caching-management-cache-mode-query-native-example[]
			List<Person> persons = session.createQuery(
				"select p from Person p" )
			.setCacheable( true )
			.setCacheMode( CacheMode.REFRESH )
			.list();
			//end::caching-management-cache-mode-query-native-example[]

			//tag::caching-management-evict-native-example[]
			session.getSessionFactory().getCache().evictQueryRegion( "query.cache.person" );
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
