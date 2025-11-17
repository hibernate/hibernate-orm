/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.array;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Struct;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsStructAggregate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsTypedArrays;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = {
		StructArrayWithNullElementTestDemoTest.Book.class,
		StructArrayWithNullElementTestDemoTest.Author.class
})
@RequiresDialectFeature(feature = SupportsStructAggregate.class)
@RequiresDialectFeature(feature = SupportsTypedArrays.class)
class StructArrayWithNullElementTestDemoTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var book = new Book();
			book.id = 1;
			book.authors = Arrays.asList(
					new Author( "John", "Smith" ),
					null
			);
			session.persist( book );
		} );

		scope.inSession( session -> {
			final var book = session.find( Book.class, 1 );
			assertEquals( 2, book.authors.size() );
			assertEquals( new Author( "John", "Smith" ), book.authors.get( 0 ) );
			assertNull( book.authors.get( 1 ) );
		} );
	}

	@Entity(name = "Book")
	@Table(name = "books")
	static class Book {
		@Id
		int id;
		List<Author> authors;
	}

	@Embeddable
	@Struct(name = "Author")
	record Author(String firstName, String lastName) {
	}
}
