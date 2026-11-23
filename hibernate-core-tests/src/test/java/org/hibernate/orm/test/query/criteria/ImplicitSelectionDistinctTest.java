package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;
import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A criteria query with {@code distinct(true)} and no explicit selection
 * must apply {@code distinct} to the implicitly selected root in SQL,
 * so that a limit applies to distinct roots instead of joined rows.
 */
@DomainModel(annotatedClasses = {
		ImplicitSelectionDistinctTest.Author.class,
		ImplicitSelectionDistinctTest.Book.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-21152")
public class ImplicitSelectionDistinctTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author first = new Author( 1L, "Iain" );
			final Author second = new Author( 2L, "Ursula" );
			session.persist( first );
			session.persist( second );
			session.persist( new Book( 1L, "Consider Phlebas", first ) );
			session.persist( new Book( 2L, "Excession", first ) );
			session.persist( new Book( 3L, "The Dispossessed", second ) );
			session.persist( new Book( 4L, "The Lathe of Heaven", second ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void implicitSelection(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final CriteriaQuery<Author> query = distinctAuthorsWithBooks( session.getCriteriaBuilder(), false );
			observer.clear();
			// this overload leaves the selection implicit
			final List<Author> authors = session.createSelectionQuery( query ).setMaxResults( 2 ).getResultList();
			assertDistinct( observer, authors );
		} );
	}

	@Test
	public void implicitSelectionMadeExplicit(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final CriteriaQuery<Author> query = distinctAuthorsWithBooks( session.getCriteriaBuilder(), false );
			observer.clear();
			// this overload selects the single root explicitly
			final List<Author> authors = session.createQuery( query ).setMaxResults( 2 ).getResultList();
			assertDistinct( observer, authors );
		} );
	}

	@Test
	public void renderImplicitSelectionRequiresOneRoot(SessionFactoryScope scope) {
		final var query = scope.getSessionFactory().getCriteriaBuilder().createQuery( Author.class );
		query.distinct( true );
		final var statement = (SqmVisitableNode) query;
		assertEquals( "Criteria did not define any query roots",
				assertThrows( IllegalArgumentException.class, statement::toHqlString ).getMessage() );

		query.from( Author.class );
		query.from( Book.class );
		assertEquals( "Criteria has multiple query roots",
				assertThrows( IllegalArgumentException.class, statement::toHqlString ).getMessage() );
	}

	@Test
	public void explicitSelection(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final CriteriaQuery<Author> query = distinctAuthorsWithBooks( session.getCriteriaBuilder(), true );
			observer.clear();
			final List<Author> authors = session.createSelectionQuery( query ).setMaxResults( 2 ).getResultList();
			assertDistinct( observer, authors );
		} );
	}

	private static CriteriaQuery<Author> distinctAuthorsWithBooks(CriteriaBuilder builder, boolean explicitSelection) {
		final CriteriaQuery<Author> query = builder.createQuery( Author.class );
		final Root<Author> author = query.from( Author.class );
		author.join( "books" );
		if ( explicitSelection ) {
			query.select( author );
		}
		return query.distinct( true ).orderBy( builder.asc( author.get( "id" ) ) );
	}

	private static void assertDistinct(CollectingStatementObserver observer, List<Author> authors) {
		final List<String> queries = observer.getSqlQueries();
		assertEquals( 1, queries.size() );
		final String sql = queries.get( 0 );
		// without distinct, the limit of two rows covers only the first author
		assertEquals( 2, authors.size(), sql );
		assertTrue( sql.contains( " distinct " ), sql );
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		Long id;
		String name;
		@OneToMany(mappedBy = "author")
		List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		Long id;
		String title;
		@ManyToOne
		Author author;

		public Book() {
		}

		public Book(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}
	}
}
