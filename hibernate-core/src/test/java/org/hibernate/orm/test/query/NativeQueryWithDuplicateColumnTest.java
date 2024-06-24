/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				NativeQueryWithDuplicateColumnTest.Book.class,
				NativeQueryWithDuplicateColumnTest.Publisher.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15608")
public class NativeQueryWithDuplicateColumnTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Publisher publisher = new Publisher( "The publisher" );
					Book book = new Book( "The book with no pictures", publisher );

					session.persist( publisher );
					session.persist( book );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Book" ).executeUpdate();
					session.createMutationQuery( "delete from Publisher" ).executeUpdate();
				}
		);
	}

	@Test
	public void testNativeQueryWithAddEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery(
							"select {book.*} from BOOK_TABLE book"
					);
					query.addEntity( "book", Book.class );
					List<Book> books = query.list();

					assertThat( books.size() ).isEqualTo( 1 );

					Book book = books.get( 0 );
					assertThat( book.getPublisher().getId() ).isEqualTo( book.getPublisherFk() );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<Book> query = session.createQuery(
							"select b from Book b", Book.class
					);
					List<Book> books = query.list();

					assertThat( books.size() ).isEqualTo( 1 );

					Book book = books.get( 0 );
					assertThat( book.getPublisher().getId() ).isEqualTo( book.getPublisherFk() );
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Book> books = ( (NativeQuery) session.createNativeQuery(
							"select book.* from BOOK_TABLE book", Book.class
					) ).list();

					assertThat( books.size() ).isEqualTo( 1 );
					Book book = books.get( 0 );
					assertThat( book.getPublisher().getId() ).isEqualTo( book.getPublisherFk() );
				}
		);
	}

	@Entity(name = "Book")
	@Table(name = "BOOK_TABLE")
	public static class Book {
		@Id
		@GeneratedValue
		@Column(name = "book_id")
		private Long id;

		@ManyToOne(targetEntity = Publisher.class, fetch = FetchType.LAZY)
		@JoinColumn(name = "publisher_fk")
		private Publisher publisher;

		@Column(name = "publisher_fk", nullable = false, insertable = false, updatable = false)
		@Access(AccessType.FIELD)
		private Long publisherFk;

		@Column(name = "title")
		private String title;

		public Book() {
		}

		public Book(String title, Publisher publisher) {
			this.title = title;
			this.publisher = publisher;
		}

		public Long getId() {
			return id;
		}

		public Publisher getPublisher() {
			return publisher;
		}

		public Long getPublisherFk() {
			return publisherFk;
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "Publisher")
	@Table(name = "PUBLISHER_TABLE")
	public static class Publisher {
		@Id
		@GeneratedValue
		@Column(name = "PUBLISHER_ID")
		private Long id;

		@Column(name = "DESCRIPTION")
		private String description;

		public Publisher() {
		}

		public Publisher(String description) {
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

	}
}
