/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations.genericentity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that {@link org.hibernate.annotations.ConverterRegistration} works
 * correctly when an entity has a generic type parameter bounded by another
 * class (e.g. {@code Book<T extends Person>}).
 *
 * @author Vincent Bouthinon
 * @author Marco Belladelli
 */
@DomainModel(
		annotatedClasses = {
				ConverterRegistrationWithGenericEntityTest.Book.class,
				ConverterRegistrationWithGenericEntityTest.Novel.class,
		},
		annotatedPackageNames = "org.hibernate.orm.test.mapping.converted.converter.registrations.genericentity"
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20276")
class ConverterRegistrationWithGenericEntityTest {

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book<>();
			book.setTitle( "Dune" );
			session.persist( book );
			final var novel = new Novel();
			novel.setTitle( "Neuromancer" );
			novel.setGenre( "Cyberpunk" );
			session.persist( novel );
		} );
	}

	@Test
	void testConverter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.createSelectionQuery( "from Book where title = 'Dune'", Book.class )
					.getSingleResult();
			assertThat( book.getTitle() ).isEqualTo( "Dune" );
			final var novel = session.createSelectionQuery( "from Novel", Novel.class ).getSingleResult();
			assertThat( novel.getTitle() ).isEqualTo( "Neuromancer" );
			assertThat( novel.getGenre() ).isEqualTo( "Cyberpunk" );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	public static class Person {
	}

	@Entity(name = "Book")
	public static class Book<T extends Person> {
		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Novel")
	public static class Novel extends Book<Person> {
		private String genre;

		public String getGenre() {
			return genre;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}
	}

	@Converter(autoApply = true)
	public static class StringConverter implements AttributeConverter<String, String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : "[" + attribute + "]";
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData == null || dbData.length() < 2 ? dbData : dbData.substring( 1, dbData.length() - 1 );
		}
	}
}
