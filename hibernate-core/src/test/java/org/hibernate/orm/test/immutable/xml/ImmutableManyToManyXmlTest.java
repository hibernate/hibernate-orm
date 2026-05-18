/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.xml;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.PersistenceException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(xmlMappings = "mappings/immutable/ImmutableManyToManyXmlTest.orm.xml")
@SessionFactory
public class ImmutableManyToManyXmlTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testImmutableManyToManyCollection(SessionFactoryScope scope) {
		var book = new Book();
		book.setTitle( "Hibernate in Action" );

		var gavin = new BookAuthor();
		gavin.setName( "Gavin King" );
		var christian = new BookAuthor();
		christian.setName( "Christian Bauer" );

		book.getAuthors().add( gavin );
		book.getAuthors().add( christian );

		scope.inTransaction( session -> {
			session.persist( gavin );
			session.persist( christian );
			session.persist( book );
		} );

		PersistenceException ex = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Book b = session.find( Book.class, book.getId() );
					assertThat( b.getAuthors() ).hasSize( 2 );

					BookAuthor newAuthor = new BookAuthor();
					newAuthor.setName( "New Author" );
					session.persist( newAuthor );
					b.getAuthors().add( newAuthor );
				}
		) );
		assertThat( ex.getMessage() ).contains( "Immutable collection was modified" );

		ex = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Book b = session.find( Book.class, book.getId() );
					assertThat( b.getAuthors() ).hasSize( 2 );
					b.getAuthors().remove( b.getAuthors().iterator().next() );
				}
		) );
		assertThat( ex.getMessage() ).contains( "Immutable collection was modified" );

		scope.inTransaction(
				session -> {
					Book b = session.find( Book.class, book.getId() );
					assertThat( b.getAuthors() ).hasSize( 2 );
				}
		);
	}

	public static class Book {
		private int id;
		private String title;
		private Set<BookAuthor> authors = new HashSet<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Set<BookAuthor> getAuthors() {
			return authors;
		}

		public void setAuthors(Set<BookAuthor> authors) {
			this.authors = authors;
		}
	}

	public static class BookAuthor {
		private int id;
		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
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
