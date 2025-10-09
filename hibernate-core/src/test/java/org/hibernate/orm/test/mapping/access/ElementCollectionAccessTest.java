/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {ElementCollectionAccessTest.Book.class})
public class ElementCollectionAccessTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.getAuthors().add(new Author(
				"Vlad",
				"Mihalcea"
			));

			entityManager.persist(book);
		});
	}

	//tag::access-element-collection-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@ElementCollection
		@CollectionTable(
			name = "book_author",
			joinColumns = @JoinColumn(name = "book_id")
		)
		private List<Author> authors = new ArrayList<>();

		//Getters and setters are omitted for brevity
	//end::access-element-collection-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Author> getAuthors() {
			return authors;
		}
	//tag::access-element-collection-mapping-example[]
	}
	//end::access-element-collection-mapping-example[]

	//tag::access-embeddable-mapping-example[]
	@Embeddable
	@Access(AccessType.PROPERTY)
	public static class Author {

		private String firstName;

		private String lastName;

		public Author() {
		}

		public Author(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}
	//end::access-embeddable-mapping-example[]

}
