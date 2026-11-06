package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the query plan cache correctly maintains separate entries
 * for paginated and unpaginated queries with collection fetches.
 */
@DomainModel(annotatedClasses = {
		PaginationPlanCacheStatsTest.Parent.class,
		PaginationPlanCacheStatsTest.Child.class
})
@SessionFactory(generateStatistics = true)
public class PaginationPlanCacheStatsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 1; i <= 5; i++ ) {
				Parent parent = new Parent();
				parent.id = i;
				parent.children = new HashSet<>();
				session.persist( parent );

				for ( long j = 1; j <= 2; j++ ) {
					Child child = new Child();
					child.id = i * 10 + j;
					child.parent = parent;
					parent.children.add( child );
					session.persist( child );
				}
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void verifyPaginatedAndUnpaginatedQueriesHaveSeparateCacheEntries(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		long initialPlanCacheSize = stats.getQueryPlanCacheHitCount() + stats.getQueryPlanCacheMissCount();

		scope.inTransaction( session -> {
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count() );
		} );

		long afterFirstQuery = stats.getQueryPlanCacheHitCount() + stats.getQueryPlanCacheMissCount();
		assertTrue( afterFirstQuery > initialPlanCacheSize,
				"First query should add to plan cache" );

		scope.inTransaction( session -> {
			List<Parent> limited = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> distinctIds = limited.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 2, distinctIds.size() );
		} );

		long afterSecondQuery = stats.getQueryPlanCacheHitCount() + stats.getQueryPlanCacheMissCount();
		assertTrue( afterSecondQuery > afterFirstQuery,
				"Paginated query should add a separate plan cache entry" );

		scope.inTransaction( session -> {
			List<Parent> unlimitedAgain = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimitedAgain.stream().map( p -> p.id ).distinct().count() );
		} );

		long afterThirdQuery = stats.getQueryPlanCacheHitCount() + stats.getQueryPlanCacheMissCount();
		long thirdQueryHits = stats.getQueryPlanCacheHitCount();

		assertTrue( thirdQueryHits > 0,
				"Third query (unpaginated) should reuse the first unpaginated plan (cache hit)" );

		scope.inTransaction( session -> {
			List<Parent> limitedAgain = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> distinctIds = limitedAgain.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 2, distinctIds.size() );
		} );

		long finalHits = stats.getQueryPlanCacheHitCount();
		assertTrue( finalHits > thirdQueryHits,
				"Fourth query (paginated with different values) should reuse the paginated plan (cache hit)" );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Long id;

		@OneToMany(mappedBy = "parent")
		Set<Child> children;
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		Long id;

		@ManyToOne
		Parent parent;
	}
}
