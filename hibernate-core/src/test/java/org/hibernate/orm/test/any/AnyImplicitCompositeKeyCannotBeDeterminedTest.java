/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import org.hibernate.MappingException;
import org.hibernate.annotations.Any;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

/**
 * Verifies that implicit key type resolution for @Any mappings fails
 * when the target entity uses a composite identifier.
 * <p>
 * In this case, Hibernate cannot infer the key Java type automatically,
 * and an explicit @AnyKeyJavaClass (or @AnyKeyJavaType) must be provided.
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {AnyImplicitCompositeKeyCannotBeDeterminedTest.Book.class})
@JiraKey("HHH-20319")
class AnyImplicitCompositeKeyCannotBeDeterminedTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
		MappingException ex = Assertions.assertThrows( MappingException.class,
				() -> scope.inTransaction( em -> {
				} ) );

		Assertions.assertTrue( ex.getMessage().contains( "Could not infer key-type for `@Any` mapping" ),
				"Expected message about missing @AnyKeyJavaType / @AnyKeyJavaClass, got: " + ex.getMessage() );
	}

	@Entity(name = "book")
	public static class Book {

		@EmbeddedId
		private BookId id;

		@Any
		@JoinColumn(name = "origin_id")
		@Column(name = "origin_type")
		private Object origin;

		public Book(BookId bookId) {
			this.id = bookId;
		}

	}

	@Embeddable
	public static class BookId implements Serializable {

		@Column(name = "isbn")
		private String isbn;

		@Column(name = "language_code")
		private String languageCode;
	}
}
