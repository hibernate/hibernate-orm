/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = MapJoinTests.Book.class)
class MapJoinTests {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( "en", book );
			s.persist( book );
			var t =
					s.createQuery( "select t from Book b join b.translations t", Book.class )
							.getSingleResult();
			assertEquals(book, t);
			var title =
					s.createQuery( "select t.title from Book b join b.translations t", String.class )
							.getSingleResult();
			assertEquals("Hibernate in Action", title);
		} );
	}

	@Test void testValue(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( "en", book );
			s.persist( book );
			var b =
					s.createQuery( "select t from Book b join value(b.translations) t", Book.class )
							.getSingleResult();
			assertEquals(book, b);
			var title =
					s.createQuery( "select t.title from Book b join value(b.translations) t", String.class )
							.getSingleResult();
			assertEquals("Hibernate in Action", title);
		} );
	}

	@Test void testKey(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( "en", book );
			s.persist( book );
			var lang =
					s.createQuery( "select l from Book b join key(b.translations) l", String.class )
							.getSingleResult();
			assertEquals("en", lang);
		} );
	}

	@Test void testKeyInWhereClause(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.translations = Map.of( "en", book, "de", book );
			s.persist( book );
			var lang =
					s.createQuery( "select l from Book b join key(b.translations) l where l = 'en'", String.class )
							.getSingleResult();
			assertEquals( "en", lang );
		} );
	}

	@Test void testKeyMultipleEntries(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.translations = Map.of( "en", book, "de", book, "fr", book );
			s.persist( book );
			var langs =
					s.createQuery( "select l from Book b join key(b.translations) l order by l", String.class )
							.getResultList();
			assertEquals( 3, langs.size() );
			assertEquals( List.of( "de", "en", "fr" ), langs );
		} );
	}

	@Test void testKeyWithIndex(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.translations = Map.of( "en", book );
			s.persist( book );
			var lang =
					s.createQuery( "select i from Book b join index(b.translations) i", String.class )
							.getSingleResult();
			assertEquals( "en", lang );
		} );
	}

	@Test void testKeyElementCollection(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.metadata = Map.of( "authors", "Christian Bauer, Gavin King, Gary Gregory", "edition", "2.0" );
			s.persist( book );
			var keys =
					s.createQuery( "select k from Book b join key(b.metadata) k order by k", String.class )
							.getResultList();
			assertEquals( 2, keys.size() );
			assertEquals( List.of( "authors", "edition" ), keys );
		} );
	}

	@Entity(name="Book")
	static class Book {
		@Id String isbn;
		String title;
		@ManyToMany
		Map<String, Book> translations;
		@ElementCollection
		Map<String, String> metadata;
	}
}
