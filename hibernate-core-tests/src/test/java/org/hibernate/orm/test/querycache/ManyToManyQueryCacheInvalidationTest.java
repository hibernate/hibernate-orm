package org.hibernate.orm.test.querycache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Query cache invalidation and autoflush must account for the many-to-many join
/// table even when neither associated entity has any scalar changes.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		ManyToManyQueryCacheInvalidationTest.Owner.class,
		ManyToManyQueryCacheInvalidationTest.Element.class
})
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
})
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-10543")
public class ManyToManyQueryCacheInvalidationTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Element first = new Element();
			first.id = 1L;
			session.persist( first );
			Element second = new Element();
			second.id = 2L;
			session.persist( second );
			Owner owner = new Owner();
			owner.id = 1L;
			owner.elements.add( first );
			session.persist( owner );
		} );
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();
	}

	@Test
	void testInvalidationAfterCollectionOnlyCommit(SessionFactoryScope scope) {
		warmQueryCache( scope );
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			owner.elements.add( session.find( Element.class, 2L ) );
		} );

		scope.inTransaction( session -> assertEquals( List.of( 1L, 2L ), queryElementIds( session ) ) );
		Statistics statistics = scope.getSessionFactory().getStatistics();
		assertEquals( 2, statistics.getQueryCacheMissCount() );
		assertEquals( 1, statistics.getQueryCacheHitCount() );
		assertEquals( 2, statistics.getQueryCachePutCount() );
		assertCollectionOnlyUpdate( statistics );

		scope.inTransaction( session -> assertEquals( List.of( 1L, 2L ), queryElementIds( session ) ) );
		assertEquals( 2, statistics.getQueryCacheHitCount() );
	}

	@Test
	void testAutoflushBeforeCachedQuery(SessionFactoryScope scope) {
		warmQueryCache( scope );
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			owner.elements.add( session.find( Element.class, 2L ) );
			// The query must flush the collection change before checking the cached result.
			assertEquals( List.of( 1L, 2L ), queryElementIds( session ) );
		} );

		Statistics statistics = scope.getSessionFactory().getStatistics();
		assertEquals( 2, statistics.getQueryCacheMissCount() );
		assertEquals( 1, statistics.getQueryCacheHitCount() );
		assertCollectionOnlyUpdate( statistics );
	}

	private static void warmQueryCache(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 1L ), queryElementIds( session ) ) );
		scope.inTransaction( session -> assertEquals( List.of( 1L ), queryElementIds( session ) ) );
		Statistics statistics = scope.getSessionFactory().getStatistics();
		assertEquals( 1, statistics.getQueryCacheMissCount() );
		assertEquals( 1, statistics.getQueryCachePutCount() );
		assertEquals( 1, statistics.getQueryCacheHitCount() );
	}

	private static List<Long> queryElementIds(Session session) {
		return session.createQuery(
				"select e.id from QueryCacheOwner o join o.elements e where o.id = :id order by e.id",
				Long.class
		).setParameter( "id", 1L ).setCacheable( true ).getResultList();
	}

	private static void assertCollectionOnlyUpdate(Statistics statistics) {
		assertEquals( 0, statistics.getEntityInsertCount() );
		assertEquals( 0, statistics.getEntityUpdateCount() );
		assertEquals( 0, statistics.getEntityDeleteCount() );
		assertEquals( 1, statistics.getCollectionUpdateCount() );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictQueryRegions();
	}

	@Entity(name = "QueryCacheOwner")
	@Table(name = "query_cache_owner")
	static class Owner {
		@Id
		private Long id;

		@ManyToMany
		@JoinTable(name = "query_cache_owner_element",
				joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "element_id"))
		private Set<Element> elements = new HashSet<>();
	}

	@Entity(name = "QueryCacheElement")
	@Table(name = "query_cache_element")
	static class Element {
		@Id
		private Long id;
	}
}
