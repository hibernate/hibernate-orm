/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.filter;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.WhereJoinTable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				WhereJoinTableTests.Book.class,
				WhereJoinTableTests.Reader.class
		}
)
@SessionFactory
public class WhereJoinTableTests {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try ( Statement statement = connection.createStatement() ) {
					statement.executeUpdate(
							"ALTER TABLE Book_Reader ADD created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
					);
				}
			} );

			final Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vad Mihalcea" );
			session.persist( book );

			final Reader reader1 = new Reader();
			reader1.setId( 1L );
			reader1.setName( "John Doe" );
			session.persist( reader1 );

			final Reader reader2 = new Reader();
			reader2.setId( 2L );
			reader2.setName( "John Doe Jr." );
			session.persist( reader2 );
		} );
	}

	@Test
	void testWhereJoinTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try ( Statement statement = connection.createStatement() ) {
					statement.executeUpdate(
							"INSERT INTO Book_Reader " +
									"	(book_id, reader_id) " +
									"VALUES " +
									"	(1, 1) "
					);
					statement.executeUpdate(
							"INSERT INTO Book_Reader " +
									"	(book_id, reader_id, created_on) " +
									"VALUES " +
									"	(1, 2, DATEADD( 'DAY', -10, CURRENT_TIMESTAMP() )) "
					);
				}}
			);

			final Book book = session.createQuery( "select b from Book b where b.id = :id", Book.class)
					.setParameter( "id", 1L ).uniqueResult();
			assertThat( book.getCurrentWeekReaders().size(), is( 1 ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@ManyToMany
		@JoinTable(
				name = "Book_Reader",
				joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "reader_id")
		)
		@WhereJoinTable( clause = "created_on > DATEADD( 'DAY', -7, CURRENT_TIMESTAMP() )")
		private List<Reader> currentWeekReaders = new ArrayList<>();

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

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public List<Reader> getCurrentWeekReaders() {
			return currentWeekReaders;
		}

	}

	@Entity(name = "Reader")
	public static class Reader {

		@Id
		private Long id;

		private String name;

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
	}
}
