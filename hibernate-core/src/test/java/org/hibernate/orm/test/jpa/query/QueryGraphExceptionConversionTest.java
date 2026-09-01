/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(annotatedClasses = {
		QueryGraphExceptionConversionTest.Book.class,
		QueryGraphExceptionConversionTest.Publisher.class
})
class QueryGraphExceptionConversionTest {
	@Test
	void incompatibleGraphThrowsIllegalArgumentException(EntityManagerFactoryScope scope) {
		try ( var entityAgent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var publisherGraph = entityAgent.createEntityGraph( Publisher.class );

			final var exception = assertThrows(
					IllegalArgumentException.class,
					() -> entityAgent.createQuery( "select b from Book b", publisherGraph )
			);
			assertInstanceOf( QueryTypeMismatchException.class, exception.getCause() );
		}
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		private Long id;
	}

	@Entity(name = "Publisher")
	static class Publisher {
		@Id
		private Long id;
	}
}
