package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproducer for HHH-20943: pagination limit silently dropped when the same HQL
 * is first executed without pagination and then with setFirstResult/setMaxResults.
 * The query plan cache reuses the unlimited plan for the limited execution.
 */
@DomainModel(annotatedClasses = {
		PaginationPlanCacheTest.Parent.class,
		PaginationPlanCacheTest.Child.class
})
@SessionFactory
public class PaginationPlanCacheTest {

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
	public void limitIsHonouredWhenTheSameHqlRanUnpaginatedBefore(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			// First execution: no pagination
			// This should create a query plan without derived table transformation
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count(),
					"Unlimited query should return all 5 parents" );
		} );

		// Clear the session to ensure we're in a fresh state
		scope.inTransaction( session -> {
			// Second execution: with pagination
			// Bug: this reuses the cached plan from the unlimited execution,
			// which doesn't have the derived table transformation,
			// so the limit is silently ignored
			List<Parent> limited = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> distinctIds = limited.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 2, distinctIds.size(),
					"Limited query should return exactly 2 parents, but got: " + distinctIds );
			assertEquals( List.of( 1L, 2L ), distinctIds,
					"Limited query should return parents [1, 2], but got: " + distinctIds );
		} );
	}

	@Test
	public void unpaginatedQueryReturnsEverythingWhenTheSameHqlRanPaginatedBefore(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			List<Parent> limited = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> distinctIds = limited.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 2, distinctIds.size(),
					"Limited query should return exactly 2 parents" );
		} );

		scope.inTransaction( session -> {
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count(),
					"Unlimited query should return all 5 parents" );
		} );
	}

	@Test
	public void offsetOnlyQueryShouldUsePaginatedPlan(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count(),
					"Unlimited query should return all 5 parents" );
		} );

		scope.inTransaction( session -> {
			List<Parent> offsetOnly = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 2 )
					.getResultList();

			List<Long> distinctIds = offsetOnly.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 3, distinctIds.size(),
					"Offset-only query should return 3 parents (skipping first 2)" );
			assertEquals( List.of( 3L, 4L, 5L ), distinctIds,
					"Offset-only query should return parents [3, 4, 5], but got: " + distinctIds );
		} );
	}

	@Test
	public void maxResultsOnlyQueryShouldUsePaginatedPlan(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count(),
					"Unlimited query should return all 5 parents" );
		} );

		scope.inTransaction( session -> {
			List<Parent> limitOnly = session.createSelectionQuery( hql, Parent.class )
					.setMaxResults( 3 )
					.getResultList();

			List<Long> distinctIds = limitOnly.stream().map( p -> p.id ).distinct().toList();
			assertEquals( 3, distinctIds.size(),
					"Limit-only query should return exactly 3 parents" );
			assertEquals( List.of( 1L, 2L, 3L ), distinctIds,
					"Limit-only query should return parents [1, 2, 3], but got: " + distinctIds );
		} );
	}

	@Test
	public void differentPaginationValuesShouldSharePaginatedPlan(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			List<Parent> firstPage = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> firstIds = firstPage.stream().map( p -> p.id ).distinct().toList();
			assertEquals( List.of( 1L, 2L ), firstIds,
					"First page should return parents [1, 2]" );
		} );

		scope.inTransaction( session -> {
			List<Parent> secondPage = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> secondIds = secondPage.stream().map( p -> p.id ).distinct().toList();
			assertEquals( List.of( 3L, 4L ), secondIds,
					"Second page should return parents [3, 4], but got: " + secondIds );
		} );

		scope.inTransaction( session -> {
			List<Parent> differentLimit = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 3 )
					.getResultList();

			List<Long> thirdIds = differentLimit.stream().map( p -> p.id ).distinct().toList();
			assertEquals( List.of( 1L, 2L, 3L ), thirdIds,
					"Different limit should return parents [1, 2, 3], but got: " + thirdIds );
		} );
	}

	@Test
	public void zeroOffsetWithLimitShouldWork(SessionFactoryScope scope) {
		String hql = "select p from Parent p left join fetch p.children order by p.id";

		scope.inTransaction( session -> {
			List<Parent> unlimited = session.createSelectionQuery( hql, Parent.class )
					.getResultList();
			assertEquals( 5, unlimited.stream().map( p -> p.id ).distinct().count() );
		} );

		scope.inTransaction( session -> {
			List<Parent> withZeroOffset = session.createSelectionQuery( hql, Parent.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			List<Long> distinctIds = withZeroOffset.stream().map( p -> p.id ).distinct().toList();
			assertEquals( List.of( 1L, 2L ), distinctIds,
					"Zero offset with limit should return parents [1, 2], but got: " + distinctIds );
		} );
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
