package org.hibernate.orm.test.caching;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable examples of cache reuse, invalidation, and refresh for the User Guide.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { CachingGuideTests.Person.class, CachingGuideTests.Snapshot.class })
@SessionFactory(generateStatistics = true, useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = CacheSettings.CACHE_REGION_FACTORY, value = "jcache"),
		@Setting(name = CacheSettings.USE_QUERY_CACHE, value = "true")
})
public class CachingGuideTests {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( 1L, "A", "Alpha" ) );
			session.persist( new Person( 2L, "B", "Beta" ) );
			session.persist( new Snapshot( 1L, "Snapshot" ) );
		} );
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.getSessionFactory().getStatistics().clear();
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	void entityReuseAcrossContexts(SessionFactoryScope scope) {
		Person first = scope.fromTransaction( session -> {
			EntityManager entityManager = session;
			//tag::caching-guide-entity-load[]
			Person person = entityManager.find( Person.class, 1L );
			//end::caching-guide-entity-load[]
			return person;
		} );
		var statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );
		scope.getCollectingStatementObserver().clear();
		Person second = scope.fromTransaction( session -> session.find( Person.class, 1L ) );
		assertThat( second ).isNotSameAs( first );
		assertThat( second.name ).isEqualTo( first.name );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
	}

	@Test
	void naturalIdResolutionSurvivesEntityEviction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::caching-guide-natural-id[]
			Person person = session.byNaturalId( Person.class )
					.using( "code", "A" ).load();
			//end::caching-guide-natural-id[]
			assertThat( person.name ).isEqualTo( "Alpha" );
		} );
		var statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.getNaturalIdCachePutCount() ).isEqualTo( 1 );
		scope.getSessionFactory().getCache().evictEntityData( Person.class );
		statistics.clear();
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> assertThat(
				session.byNaturalId( Person.class ).using( "code", "A" ).load().name
		).isEqualTo( "Alpha" ) );
		assertThat( statistics.getNaturalIdCacheHitCount() ).isEqualTo( 1 );
		assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 1 );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).hasSize( 1 );
	}

	@Test
	void collectionReuseAndInvalidation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = session.find( Person.class, 1L );
			person.phones.add( "123-456" );
		} );
		scope.inTransaction( session -> assertThat( session.find( Person.class, 1L ).phones )
				.containsExactly( "123-456" ) );
		scope.getSessionFactory().getStatistics().clear();
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::caching-guide-collection[]
			Person person = entityManager.find( Person.class, 1L );
			List<String> phones = person.phones;
			phones.size(); // initializes the collection, using its cache entry if available
			//end::caching-guide-collection[]
			assertThat( phones ).containsExactly( "123-456" );
		} );
		assertThat( scope.getSessionFactory().getStatistics()
				.getDomainDataRegionStatistics( "guide.phones" ).getHitCount() ).isEqualTo( 1 );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
		scope.inTransaction( session -> session.find( Person.class, 1L ).phones.add( "654-321" ) );
		scope.getSessionFactory().getStatistics().clear();
		scope.inTransaction( session -> assertThat( session.find( Person.class, 1L ).phones )
				.containsExactly( "123-456", "654-321" ) );
		assertThat( scope.getSessionFactory().getStatistics()
				.getDomainDataRegionStatistics( "guide.phones" ).getMissCount() ).isEqualTo( 1 );
	}

	@Test
	void queryKeysAndMutationInvalidation(SessionFactoryScope scope) {
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 1L, CacheMode.NORMAL ) ) )
				.containsExactly( "Alpha" );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 1L, CacheMode.NORMAL ) ) )
				.containsExactly( "Alpha" );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 2L, CacheMode.NORMAL ) ) )
				.containsExactly( "Beta" );
		var statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 2 );
		scope.inTransaction( session -> session.createMutationQuery(
				"update CacheGuidePerson set name = 'Changed' where id = 1" ).executeUpdate() );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 1L, CacheMode.NORMAL ) ) )
				.containsExactly( "Changed" );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 3 );
		// Table-level invalidation also affects the result whose row did not change.
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 2L, CacheMode.NORMAL ) ) )
				.containsExactly( "Beta" );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 4 );
	}

	@Test
	void queryRefreshOnlyReplacesTheExecutedKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			names( session, 1L, CacheMode.NORMAL );
			names( session, 2L, CacheMode.NORMAL );
		} );
		// Deliberately bypass Hibernate invalidation, simulating an external writer.
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.executeUpdate( "update cache_guide_person set name = 'External' where id in (1, 2)" );
			}
		} ) );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 1L, CacheMode.REFRESH ) ) )
				.containsExactly( "External" );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 2L, CacheMode.NORMAL ) ) )
				.containsExactly( "Beta" );
		assertThat( scope.<List<String>>fromTransaction( session -> names( session, 2L, CacheMode.REFRESH ) ) )
				.containsExactly( "External" );
	}

	private static List<String> names(Session session, long id, CacheMode mode) {
		return session.createQuery( "select p.name from CacheGuidePerson p where p.id = :id", String.class )
				.setParameter( "id", id ).setCacheable( true ).setCacheRegion( "guide.names" )
				.setCacheMode( mode ).getResultList();
	}

	@Test
	void queryLayouts(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "from CacheGuidePerson where id = 1", Person.class )
				.setCacheable( true ).getResultList() );
		scope.getSessionFactory().getCache().evictEntityData( Person.class );
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from CacheGuidePerson where id = 1", Person.class ).setCacheable( true )
				.getSingleResult().name ).isEqualTo( "Alpha" ) );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).hasSize( 1 );
		scope.inTransaction( session -> session.createQuery( "from CacheGuideSnapshot", Snapshot.class )
				.setCacheable( true ).getResultList() );
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> assertThat( session.createQuery( "from CacheGuideSnapshot", Snapshot.class )
				.setCacheable( true ).getSingleResult().name ).isEqualTo( "Snapshot" ) );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
	}

	@Test
	void refreshAndEvictionDoNotReplaceManagedState(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person managed = session.find( Person.class, 1L );
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "update cache_guide_person set name = 'External' where id = 1" );
				}
			} );
			session.getSessionFactory().getCache().evictEntityData( Person.class );
			assertThat( managed.name ).isEqualTo( "Alpha" );
			//tag::caching-guide-refresh-session[]
			Person unchanged = session.createQuery( "from CacheGuidePerson where id = 1", Person.class )
					.setCacheMode( CacheMode.REFRESH ).getSingleResult();
			assertThat( unchanged.name ).isEqualTo( "Alpha" );
			Person refreshed = session.createQuery( "from CacheGuidePerson where id = 1", Person.class )
					.setCacheMode( CacheMode.REFRESH_SESSION ).getSingleResult();
			//end::caching-guide-refresh-session[]
			assertThat( unchanged ).isSameAs( managed );
			assertThat( refreshed ).isSameAs( managed );
			assertThat( refreshed.name ).isEqualTo( "External" );
		} );
	}

	@Test
	void standardFindModes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::caching-guide-find-options[]
			Person person = entityManager.find( Person.class, 1L,
					CacheRetrieveMode.BYPASS, CacheStoreMode.REFRESH );
			//end::caching-guide-find-options[]
			assertThat( person.name ).isEqualTo( "Alpha" );
		} );
	}

	@Test
	void queryStatistics(SessionFactoryScope scope) {
		scope.inTransaction( session -> names( session, 1L, CacheMode.NORMAL ) );
		scope.inTransaction( session -> names( session, 1L, CacheMode.NORMAL ) );
		var sessionFactory = scope.getSessionFactory();
		//tag::caching-guide-statistics[]
		var statistics = sessionFactory.getStatistics();
		var region = statistics.getQueryRegionStatistics( "guide.names" );
		long hits = region == null ? 0 : region.getHitCount();
		long misses = region == null ? 0 : region.getMissCount();
		double hitRatio = hits + misses == 0 ? 0.0 : (double) hits / (hits + misses);
		//end::caching-guide-statistics[]
		assertThat( hits ).isEqualTo( 1 );
		assertThat( misses ).isEqualTo( 1 );
		assertThat( hitRatio ).isEqualTo( 0.5 );
	}

	@Test
	void queryOptionsAndEviction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::caching-guide-query-standard[]
			List<Person> people = entityManager.createQuery(
					"from CacheGuidePerson where name = :name", Person.class )
					.setParameter( "name", "Alpha" )
					.setHint( "org.hibernate.cacheable", true )
					.getResultList();
			//end::caching-guide-query-standard[]
			assertThat( people ).hasSize( 1 );
			//tag::caching-guide-query-native[]
			List<Person> cachedPeople = session.createQuery(
					"from CacheGuidePerson where name = :name", Person.class )
					.setParameter( "name", "Alpha" ).setCacheable( true ).getResultList();
			//end::caching-guide-query-native[]
			assertThat( cachedPeople ).hasSize( 1 );
			//tag::caching-guide-query-region[]
			List<String> names = entityManager.createQuery(
					"select p.name from CacheGuidePerson p where p.id = :id", String.class )
					.setParameter( "id", 1L )
					.setHint( "org.hibernate.cacheable", true )
					.setHint( "org.hibernate.cacheRegion", "guide.names" )
					.getResultList();
			//end::caching-guide-query-region[]
			assertThat( names ).containsExactly( "Alpha" );
			//tag::caching-guide-query-modes[]
			List<Person> refreshedPeople = entityManager.createQuery( "from CacheGuidePerson", Person.class )
					.setCacheRetrieveMode( CacheRetrieveMode.BYPASS )
					.setCacheStoreMode( CacheStoreMode.REFRESH )
					.getResultList();
			//end::caching-guide-query-modes[]
			assertThat( refreshedPeople ).hasSize( 2 );
			//tag::caching-guide-query-refresh[]
			List<String> refreshedNames = session.createQuery(
					"select p.name from CacheGuidePerson p where p.id = :id", String.class )
					.setParameter( "id", 1L ).setCacheable( true )
					.setCacheRegion( "guide.names" ).setCacheMode( CacheMode.REFRESH )
					.getResultList();
			//end::caching-guide-query-refresh[]
			assertThat( refreshedNames ).containsExactly( "Alpha" );
			//tag::caching-guide-evict-standard[]
			entityManager.getEntityManagerFactory().getCache().evict( Person.class, 1L );
			//end::caching-guide-evict-standard[]
			//tag::caching-guide-evict-native[]
			var cache = session.getSessionFactory().getCache();
			cache.evictEntityData( Person.class, 1L );
			cache.evictNaturalIdData( Person.class );
			cache.evictCollectionData( Person.class.getName() + ".phones", 1L );
			cache.evictQueryRegion( "guide.names" );
			//end::caching-guide-evict-native[]
			assertThat( cache.containsEntity( Person.class, 1L ) ).isFalse();
		} );
	}

	//tag::caching-guide-mapping[]
	@Entity(name = "CacheGuidePerson")
	@Table(name = "cache_guide_person")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "guide.person", includeLazy = false)
	@NaturalIdCache(region = "guide.person.natural")
	@QueryCacheLayout(layout = CacheLayout.SHALLOW)
	public static class Person {
		@Id
		Long id;
		@NaturalId
		String code;
		String name;
		@Version
		int version;
		@ElementCollection
		@OrderColumn
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "guide.phones")
		List<String> phones = new ArrayList<>();
	//end::caching-guide-mapping[]

		protected Person() {
		}

		Person(long id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}
	//tag::caching-guide-mapping[]
	}
	//end::caching-guide-mapping[]

	//tag::caching-guide-layout[]
	@Entity(name = "CacheGuideSnapshot")
	@Table(name = "cache_guide_snapshot")
	@QueryCacheLayout(layout = CacheLayout.FULL)
	public static class Snapshot {
		@Id
		Long id;
		String name;
	//end::caching-guide-layout[]

		protected Snapshot() {
		}

		Snapshot(long id, String name) {
			this.id = id;
			this.name = name;
		}
	//tag::caching-guide-layout[]
	}
	//end::caching-guide-layout[]
}
