/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When neither {@link org.hibernate.annotations.AnyKeyJavaType} nor
 * {@link org.hibernate.annotations.AnyKeyJavaClass} is specified, the key type of an
 * {@link Any} association must be inferred from the identifier of the <em>target</em>
 * entities, not from the identifier of the entity <em>declaring</em> the association.
 * <p>
 * The two are unrelated: the value stored in the key column is handed straight to
 * {@code AnyType#resolve} as the identifier of the target entity. Here {@link Shelf} is
 * identified by a generated {@code Long}, while every target ({@link Book}, {@link Magazine})
 * is identified by a natural {@code String} key. The only correct inference is therefore
 * {@code String}, and it is unambiguous since all targets agree.
 * <p>
 * Inferring from {@code Shelf#id} would silently yield {@code Long}: the schema would use a
 * numeric key column and the association would only break later, at runtime.
 *
 * @author Vincent Bouthinon
 */
@DomainModel(annotatedClasses = {
		AnyImplicitKeyTypeFromTargetEntitiesTest.Shelf.class,
		AnyImplicitKeyTypeFromTargetEntitiesTest.Book.class,
		AnyImplicitKeyTypeFromTargetEntitiesTest.Magazine.class
})
@SessionFactory
@JiraKey("HHH-20319")
class AnyImplicitKeyTypeFromTargetEntitiesTest {

	private static final String ISBN = "978-2-1234-5680-3";

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void keyTypeIsInferredFromTargetEntities(DomainModelScope scope) {
		final PersistentClass shelfBinding =
				scope.getDomainModel().getEntityBinding( Shelf.class.getName() );
		final org.hibernate.mapping.Any content =
				(org.hibernate.mapping.Any) shelfBinding.getProperty( "content" ).getValue();

		assertThat( content.getKeyDescriptor().resolve().getDomainJavaType().getJavaTypeClass() )
				.as( "the '@Any' key type must be inferred from the target entities"
						+ " (Book#isbn, Magazine#issn), not from the declaring entity (Shelf#id)" )
				.isEqualTo( String.class );
	}

	@Test
	void anyAssociationRoundTrips(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = new Book( ISBN, "Hibernate in Action" );
			session.persist( book );
			final Shelf shelf = new Shelf();
			shelf.content = book;
			session.persist( shelf );
		} );

		scope.inTransaction( session -> {
			final Shelf shelf =
					session.createQuery( "from Shelf", Shelf.class ).getSingleResult();
			assertThat( shelf.content ).isInstanceOf( Book.class );
			assertThat( ( (Book) shelf.content ).isbn ).isEqualTo( ISBN );
		} );
	}

	@Entity(name = "Shelf")
	@Table(name = "SHELF")
	static class Shelf {

		@Id
		@GeneratedValue
		Long id;

		@Any
		@JoinColumn(name = "CONTENT_ID")
		@Column(name = "CONTENT_TYPE")
		@AnyDiscriminatorValue(discriminator = "B", entity = Book.class)
		@AnyDiscriminatorValue(discriminator = "M", entity = Magazine.class)
		Object content;
	}

	@Entity(name = "Book")
	@Table(name = "BOOK")
	static class Book {

		@Id
		String isbn;

		String title;

		Book() {
		}

		Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}
	}

	@Entity(name = "Magazine")
	@Table(name = "MAGAZINE")
	static class Magazine {

		@Id
		String issn;

		String title;
	}
}
