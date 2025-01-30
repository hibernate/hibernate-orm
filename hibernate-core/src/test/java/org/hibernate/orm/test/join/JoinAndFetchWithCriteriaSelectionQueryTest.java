/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.Order;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When fetching and joining associations using the criteria API, we need to check that we are not affected by the order
 * of the operations
 */
@SessionFactory
@DomainModel(annotatedClasses = { JoinAndFetchWithCriteriaSelectionQueryTest.Book.class, JoinAndFetchWithCriteriaSelectionQueryTest.Author.class })
@Jira("https://hibernate.atlassian.net/browse/HHH-19034")
class JoinAndFetchWithCriteriaSelectionQueryTest {

	static final Author amal = new Author( 1L, "Amal El-Mohtar" );
	static final Author max = new Author( 2L, "Max Gladstone" );
	static final Author ursula = new Author( 3L, "Ursula K. Le Guin" );
	static final Book timeWar = new Book( 1L, "This Is How You Lose the Time War" );
	static final Book leftHand = new Book( 2L, "The Left Hand of Darkness" );

	@BeforeAll
	public static void populateDb(SessionFactoryScope scope) {
		timeWar.getAuthors().add( amal );
		timeWar.getAuthors().add( max );
		leftHand.getAuthors().add( ursula );
		scope.inTransaction( session -> {
			session.persist( amal );
			session.persist( max );
			session.persist( ursula );
			session.persist( timeWar );
			session.persist( leftHand );
		} );
	}

	@Test
	void fetchBeforeJoinWithWhereClauseTest(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			// Find all the books from an author, and load the authors association eagerly
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Book> query = cb.createQuery( Book.class );
			Root<Book> from = query.from( Book.class );

			// The fetch MUST BE created before the join for this test
			Fetch<Object, Object> fetch = from.fetch( "authors" );
			Join<Object, Object> join = from.join( "authors" );
			query.where( cb.equal( join.get( "id" ), 2L ) );

			// Because there's a filter on the association, they need to be two distinct joins
			assertThat( join ).isNotEqualTo( fetch );

			Book book = session.createQuery( query ).getSingleResult();
			assertThat( book ).isEqualTo( timeWar );
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( book.getAuthors() ).containsExactlyInAnyOrder( amal, max );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	void fetchAfterJoinWithWhereClauseTest(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			// Find all the books from an author, and load the authors association eagerly
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Book> query = cb.createQuery( Book.class );
			Root<Book> from = query.from( Book.class );

			// The join MUST BE created before the fetch for this test
			Join<Object, Object> join = from.join( "authors" );
			Fetch<Object, Object> fetch = from.fetch( "authors" );
			query.where( cb.equal( join.get( "id" ), 2L ) );

			// Because there's a filter on the association, they need to be two distinct joins
			assertThat( join ).isNotEqualTo( fetch );
			Book book = session.createQuery( query ).getSingleResult();

			assertThat( book ).isEqualTo( timeWar );
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( book.getAuthors() ).containsExactlyInAnyOrder( amal, max );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	void fetchAfterJoinWithoutWhereClauseTest(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Book> query = cb.createQuery( Book.class );
			Root<Book> from = query.from( Book.class );

			// The join MUST BE created before the fetch for this test
			Join<Object, Object> join = from.join( "authors" );
			Fetch<Object, Object> fetch = from.fetch( "authors" );

			// The current behaviour, but we could reuse the same join in this case
			assertThat( join ).isNotEqualTo( fetch );

			EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType( Book.class );
			SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute( "title" );
			query.select( from ).distinct( true );

			List<Book> books = session
					.createSelectionQuery( query )
					.setOrder( Order.asc( title ) )
					.getResultList();
			assertThat( books ).containsExactly( leftHand, timeWar );
			assertThat( Hibernate.isInitialized( books.get( 0 ).getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( books.get( 1 ).getAuthors() ) ).isTrue();

			inspector.assertExecutedCount( 1 );
			// The current behaviour, but we could generate a query with only 2 join
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	void fetchBeforeJoinWithoutWhereClauseTest(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Book> query = cb.createQuery( Book.class );
			Root<Book> from = query.from( Book.class );

			// The fetch MUST BE created before the join for this test
			Fetch<Object, Object> fetch = from.fetch( "authors" );
			Join<Object, Object> join = from.join( "authors" );

			// The current behaviour, but we could reuse the same join in this case
			assertThat( join ).isNotEqualTo( fetch );

			EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType( Book.class );
			SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute( "title" );
			query.select( from ).distinct( true );

			List<Book> books = session
					.createSelectionQuery( query )
					.setOrder( Order.asc( title ) )
					.getResultList();
			assertThat( books ).containsExactly( leftHand, timeWar );
			assertThat( Hibernate.isInitialized( books.get( 0 ).getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( books.get( 1 ).getAuthors() ) ).isTrue();

			inspector.assertExecutedCount( 1 );
			// The current behaviour, but we could generate a query with only 2 join
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;
		private String name;

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object object) {
			if ( object == null || getClass() != object.getClass() ) {
				return false;
			}
			Author author = (Author) object;
			return Objects.equals( name, author.name );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( name );
		}

		@Override
		public String toString() {
			return id + ":" + name;
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;
		private String title;
		@OneToMany
		private List<Author> authors = new ArrayList<>();

		public Book() {
		}

		public Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Author> getAuthors() {
			return authors;
		}

		public void setAuthors(List<Author> authors) {
			this.authors = authors;
		}

		@Override
		public boolean equals(Object object) {
			if ( object == null || getClass() != object.getClass() ) {
				return false;
			}
			Book book = (Book) object;
			return Objects.equals( title, book.title );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( title );
		}

		@Override
		public String toString() {
			return id + ":" + title;
		}
	}
}
