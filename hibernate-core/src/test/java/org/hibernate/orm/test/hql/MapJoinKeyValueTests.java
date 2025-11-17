/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {MapJoinKeyValueTests.Book.class,
		MapJoinKeyValueTests.Language.class})
class MapJoinKeyValueTests {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Language language = new Language();
			language.id = "en_AU";
			language.language = "en";
			language.country = "AU";
			s.persist( language );
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( language, book );
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
			Language language = new Language();
			language.id = "en_AU";
			language.language = "en";
			language.country = "AU";
			s.persist( language );
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( language, book );
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
			Language language = new Language();
			language.id = "en_AU";
			language.language = "en";
			language.country = "AU";
			s.persist( language );
			Book book = new Book();
			book.isbn = "978-1932394153";
			book.title = "Hibernate in Action";
			book.translations = Map.of( language, book );
			s.persist( book );
			var l =
					s.createQuery( "select l from Book b join key(b.translations) l", Language.class )
							.getSingleResult();
			assertEquals(language, l);
			var c1 =
					s.createQuery( "select l.country from Book b join key(b.translations) l", String.class )
							.getSingleResult();
			assertEquals("AU", c1);
			var c2 =
					s.createQuery( "select key(t).country from Book b join b.translations t", String.class )
							.getSingleResult();
			assertEquals("AU", c2);
		} );
	}

	@Entity(name="Book")
	static class Book {
		@Id String isbn;
		String title;
		@ManyToMany
		Map<Language, Book> translations;
	}

	@Entity
	static class Language {
		@Id
		String id;
		String language;
		String country;
		String variant;
	}
}
