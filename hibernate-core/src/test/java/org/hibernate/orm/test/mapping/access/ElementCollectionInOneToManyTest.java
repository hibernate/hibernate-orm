/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey(value = "HHH-13721")
@DomainModel(
		annotatedClasses = {
				ElementCollectionInOneToManyTest.Author.class, ElementCollectionInOneToManyTest.Book.class
		}
)
@SessionFactory
public class ElementCollectionInOneToManyTest {

	@Test
	void hhh13721Test(SessionFactoryScope scope) {

		// prepare data
		scope.inTransaction( session -> {

			Chapter c1 = new Chapter();
			c1.name = "chapter1";

			Chapter c2 = new Chapter();
			c2.name = "chapter2";

			Chapter c3 = new Chapter();
			c3.name = "chapter3";

			Book book = new Book();
			book.name = "book";
			book.chapters.add( c1 );
			book.chapters.add( c2 );
			book.chapters.add( c3 );

			Author author = new Author();
			author.name = "author";
			author.books.add( book );

			session.persist( author );
		} );

		// find and assert
		scope.inTransaction( session -> {

			Author author = session.createSelectionQuery( "select a from Author a", Author.class ).getSingleResult();
			assertNotNull( author );
			assertEquals( "author", author.name );
			assertEquals( 1, author.books.size() );

			Book book = author.books.getFirst();
			assertEquals( "book", book.name );
			assertEquals( 3, book.chapters.size() );

			List<String> chapters = book.chapters.stream().map( c -> c.name ).toList();
			assertTrue( chapters.contains( "chapter1" ) );
			assertTrue( chapters.contains( "chapter2" ) );
			assertTrue( chapters.contains( "chapter3" ) );
		} );
	}
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
	@Entity(name = "Author")
	public static class Author {

		@Id
		@GeneratedValue
		long id;

		String name;

		@OneToMany(cascade = CascadeType.ALL)
		List<Book> books = new ArrayList<>();
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		long id;

		String name;

		@ElementCollection
		Collection<Chapter> chapters = new ArrayList<>();
	}

	@Embeddable
	public static class Chapter {

		String name;
	}

}
