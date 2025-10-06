/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Size;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses = {
		CollectionActionsValidationStatelessTest.Author.class,
		CollectionActionsValidationStatelessTest.Book.class,
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JAKARTA_VALIDATION_MODE, value = "auto"))
@Jira("https://hibernate.atlassian.net/browse/HHH-19843")
public class CollectionActionsValidationStatelessTest {

	@Test
	void smoke(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				ArrayList<Book> books = new ArrayList<>();
				Author author = new Author( 1L, "first", "last", books );
				Book book = new Book( 10L, "", author );
				books.add( book );

				session.upsertMultiple( List.of( author ) );
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Table(name = "author")
	@Entity
	static class Author {

		public Author() {
		}

		public Author(long id, String firstName, String lastName, List<Book> books) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.books = books;
			this.id = id;
		}

		@Id
		Long id;

		String firstName;

		String lastName;

		@OneToMany
		@JoinColumn(name = "bookId")
		@Size(min = 10)
		List<Book> books;

	}

	@Table(name = "book")
	@Entity
	static class Book {

		public Book() {
		}

		public Book(long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@Id
		Long id;

		String title;

		@ManyToOne
		Author author;
	}
}
