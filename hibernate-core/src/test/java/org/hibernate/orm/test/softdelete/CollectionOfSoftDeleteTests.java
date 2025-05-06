/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for collections which contain soft-deletable entities
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = { CollectionOfSoftDeleteTests.Shelf.class, CollectionOfSoftDeleteTests.Book.class } )
@SessionFactory(useCollectingStatementInspector = true)
public class CollectionOfSoftDeleteTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Shelf horror = new Shelf( 1, "Horror" );
			session.persist( horror );

			final Book theShining = new Book( 1, "The Shining" );
			horror.addBook( theShining );
			session.persist( theShining );

			session.flush();

			session.doWork( (connection) -> {
				final Statement statement = connection.createStatement();
				statement.execute( "update books set deleted = 'Y' where id = 1" );
			} );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testLoading(SessionFactoryScope scope) {
		final SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			final Shelf loaded = session.get( Shelf.class, 1 );
			assertThat( loaded ).isNotNull();

			assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );

			sqlInspector.clear();

			assertThat( loaded.getBooks() ).isNotNull();
			assertThat( loaded.getBooks() ).isEmpty();

			assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsAnyOf( ".deleted='N'", ".deleted=N'N'" );
		} );
	}

	@Test
	void testQueryJoin(SessionFactoryScope scope) {
		final SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			final Shelf shelf = session
					.createSelectionQuery( "from Shelf join fetch books", Shelf.class )
					.uniqueResult();
			assertThat( shelf ).isNotNull();
			assertThat( shelf.getBooks() ).isNotNull();

			assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsAnyOf( ".deleted='N'", ".deleted=N'N'" );
		} );
	}

	@Entity(name="Shelf")
	@Table(name="shelves")
	public static class Shelf {
		@Id
		private Integer id;
		private String name;
		@OneToMany
		@JoinColumn(name = "shelf_fk")
		private Collection<Book> books;

		public Shelf() {
		}

		public Shelf(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Collection<Book> getBooks() {
			return books;
		}

		public void setBooks(Collection<Book> books) {
			this.books = books;
		}

		public void addBook(Book book) {
			if ( books == null ) {
				books = new ArrayList<>();
			}
			books.add( book );
		}
	}

	@Entity(name="Book")
	@Table(name="books")
	@SoftDelete(converter = YesNoConverter.class)
	public static class Book {
		@Id
		private Integer id;
		private String name;

		public Book() {
		}

		public Book(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
