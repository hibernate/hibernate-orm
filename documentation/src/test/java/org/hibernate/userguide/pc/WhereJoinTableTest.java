/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.pc;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.Session;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class WhereJoinTableTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Reader.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::pc-where-persistence-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {

			entityManager.unwrap( Session.class ).doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate(
						"ALTER TABLE Book_Reader ADD created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
					);
				}
			} );

			//tag::pc-where-join-table-persist-example[]
			Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vad Mihalcea" );
			entityManager.persist( book );

			Reader reader1 = new Reader();
			reader1.setId( 1L );
			reader1.setName( "John Doe" );
			entityManager.persist( reader1 );

			Reader reader2 = new Reader();
			reader2.setId( 2L );
			reader2.setName( "John Doe Jr." );
			entityManager.persist( reader2 );
			//end::pc-where-join-table-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
			//tag::pc-where-join-table-persist-example[]

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
				//end::pc-where-join-table-persist-example[]
				}}
			);

			//tag::pc-where-join-table-fetch-example[]
			Book book = entityManager.find( Book.class, 1L );
			assertEquals( 1, book.getCurrentWeekReaders().size() );
			//end::pc-where-join-table-fetch-example[]
		} );
	}

	//tag::pc-where-join-table-example[]
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
		private List<Reader> currentWeekReaders = new ArrayList<>( );

		//Getters and setters omitted for brevity

		//end::pc-where-join-table-example[]
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

		//tag::pc-where-join-table-example[]
	}

	@Entity(name = "Reader")
	public static class Reader {

		@Id
		private Long id;

		private String name;

		//Getters and setters omitted for brevity

		//end::pc-where-join-table-example[]

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
		//tag::pc-where-join-table-example[]
	}
		//end::pc-where-join-table-example[]
}
