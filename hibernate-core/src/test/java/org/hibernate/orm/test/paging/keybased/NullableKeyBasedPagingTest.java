package org.hibernate.orm.test.paging.keybased;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.QuerySettings.DEFAULT_NULL_ORDERING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServiceRegistry(settings = @Setting(name = DEFAULT_NULL_ORDERING, value = "none"))
@DomainModel(annotatedClasses = NullableKeyBasedPagingTest.Book.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-21073")
class NullableKeyBasedPagingTest {

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Book" ).executeUpdate() );
	}

	@Test
	@SessionFactory
	void testAscendingNullsLast(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 4; id++ ) {
				final var book = new Book();
				book.id = id;
				book.pages = id == 4 ? null : (int) id * 100;
				session.persist( book );
			}
		} );

		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Book", Book.class );
			final var first = query.getKeyedResultList( Page.first( 2 ).keyedBy( List.of(
					Order.asc( Book.class, "pages" ).withNullsLast(),
					Order.asc( Book.class, "id" )
			) ) );
			assertEquals( List.of( 1L, 2L ), first.getResultList().stream().map( book -> book.id ).toList() );
			assertNotNull( first.getNextPage() );

			final var second = query.getKeyedResultList( first.getNextPage() );
			assertEquals( List.of( 3L, 4L ), second.getResultList().stream().map( book -> book.id ).toList() );
			assertTrue( second.isLastPage() );
		} );
	}

	@Test
	@SessionFactory
	void testAscendingNullsFirstBoundary(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var book = new Book();
				book.id = id;
				book.pages = id == 3 ? 100 : null;
				session.persist( book );
			}
		} );

		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Book", Book.class );
			final var first = query.getKeyedResultList( Page.first( 1 ).keyedBy( List.of(
					Order.asc( Book.class, "pages" ).withNullsFirst(),
					Order.asc( Book.class, "id" )
			) ) );
			assertEquals( List.of( 1L ), first.getResultList().stream().map( book -> book.id ).toList() );
			assertNotNull( first.getNextPage() );

			final var second = query.getKeyedResultList( first.getNextPage() );
			assertEquals( List.of( 2L ), second.getResultList().stream().map( book -> book.id ).toList() );
			assertNotNull( second.getNextPage() );

			final var third = query.getKeyedResultList( second.getNextPage() );
			assertEquals( List.of( 3L ), third.getResultList().stream().map( book -> book.id ).toList() );
			assertTrue( third.isLastPage() );
		} );
	}

	@Test
	@SessionFactory
	void testBackwardNullsLastBoundary(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var book = new Book();
				book.id = id;
				book.pages = id == 1 ? 100 : null;
				session.persist( book );
			}
		} );

		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Book", Book.class );
			final var first = query.getKeyedResultList( Page.first( 1 ).keyedBy( List.of(
					Order.asc( Book.class, "pages" ).withNullsLast(),
					Order.asc( Book.class, "id" )
			) ) );
			assertEquals( List.of( 1L ), first.getResultList().stream().map( book -> book.id ).toList() );
			assertNotNull( first.getNextPage() );

			final var second = query.getKeyedResultList( first.getNextPage() );
			assertEquals( List.of( 2L ), second.getResultList().stream().map( book -> book.id ).toList() );
			assertNotNull( second.getNextPage() );
			assertNotNull( second.getPreviousPage() );

			final var previous = query.getKeyedResultList( second.getPreviousPage() );
			assertEquals( List.of( 1L ), previous.getResultList().stream().map( book -> book.id ).toList() );
			assertTrue( previous.isFirstPage() );
		} );
	}

	@Test
	@SessionFactory
	void testDescendingWithRestriction(SessionFactoryScope scope) {
		persistBooks( scope, 100, 200, 300, null, null );
		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Book where id >= :minId", Book.class )
					.setParameter( "minId", 2L );
			assertTraversal( query, List.of(
					Order.desc( Book.class, "pages" ).withNullsLast(), Order.asc( Book.class, "id" )
			), List.of( 3L, 2L, 4L, 5L ) );
		} );
	}

	@Test
	@SessionFactory
	void testDialectDefaultNullPrecedence(SessionFactoryScope scope) {
		persistBooks( scope, 100, null, null );
		scope.inSession( session -> {
			final var expected = session.createSelectionQuery( "from Book order by pages, id", Book.class )
					.getResultList().stream().map( book -> book.id ).toList();
			assertTraversal( session.createSelectionQuery( "from Book", Book.class ), List.of(
					Order.asc( Book.class, "pages" ), Order.asc( Book.class, "id" )
			), expected );
		} );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = DEFAULT_NULL_ORDERING, value = "last"))
	void testConfiguredNullPrecedenceAndExplicitOverride(SessionFactoryScope scope) {
		persistBooks( scope, 100, 200, null );
		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Book", Book.class );
			assertTraversal( query, List.of(
					Order.asc( Book.class, "pages" ), Order.asc( Book.class, "id" )
			), List.of( 1L, 2L, 3L ) );
			assertTraversal( query, List.of(
					Order.asc( Book.class, "pages" ).withNullsFirst(), Order.asc( Book.class, "id" )
			), List.of( 3L, 1L, 2L ) );
		} );
	}

	private static void persistBooks(SessionFactoryScope scope, Integer... pages) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < pages.length; i++ ) {
				final var book = new Book();
				book.id = (long) i + 1;
				book.pages = pages[i];
				session.persist( book );
			}
		} );
	}

	private static void assertTraversal(
			SelectionQuery<Book> query, List<Order<? super Book>> orders, List<Long> expected) {
		var page = query.getKeyedResultList( Page.first( 1 ).keyedBy( orders ) );
		assertEquals( List.of( expected.get( 0 ) ), page.getResultList().stream().map( book -> book.id ).toList() );
		for ( int i = 1; i < expected.size(); i++ ) {
			assertNotNull( page.getNextPage() );
			page = query.getKeyedResultList( page.getNextPage() );
			assertEquals( List.of( expected.get( i ) ), page.getResultList().stream().map( book -> book.id ).toList() );
		}
		assertTrue( page.isLastPage() );
		for ( int i = expected.size() - 2; i >= 0; i-- ) {
			assertNotNull( page.getPreviousPage() );
			page = query.getKeyedResultList( page.getPreviousPage() );
			assertEquals( List.of( expected.get( i ) ), page.getResultList().stream().map( book -> book.id ).toList() );
		}
		assertTrue( page.isFirstPage() );
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		Long id;
		Integer pages;
	}
}
