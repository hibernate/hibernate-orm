/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = ListJoinTests.Book.class)
class ListJoinTests {

	@Test void testListElement(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.tags = List.of( "java", "hibernate", "jpa" );
			s.persist( book );
			var tags =
					s.createQuery( "select e from Book b join element(b.tags) e order by e", String.class )
							.getResultList();
			assertEquals( 3, tags.size() );
			assertEquals( List.of( "hibernate", "java", "jpa" ), tags );
		} );
	}

	@Test void testListIndex(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( s -> {
			Book book = new Book();
			book.isbn = "978-1932394164";
			book.title = "Java Persistence with Hibernate";
			book.tags = List.of( "java", "hibernate" );
			s.persist( book );
			var indices =
					s.createQuery( "select i from Book b join index(b.tags) i order by i", Integer.class )
							.getResultList();
			assertEquals( 2, indices.size() );
			assertEquals( List.of( 0, 1 ), indices );
		} );
	}

	@Entity(name="Book")
	static class Book {
		@Id String isbn;
		String title;
		@ElementCollection
		@OrderColumn
		List<String> tags;
	}
}
