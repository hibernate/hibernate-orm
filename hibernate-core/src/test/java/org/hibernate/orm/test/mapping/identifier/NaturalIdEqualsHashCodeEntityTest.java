/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class NaturalIdEqualsHashCodeEntityTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Library.class,
			Book.class
		};
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Library library = new Library();
			library.setId(1L);
			library.setName("Amazon");

			entityManager.persist(library);
		});
	}

	@Test
	public void testPersist() {

		//tag::entity-pojo-natural-id-equals-hashcode-persist-example[]
		Book book1 = new Book();
		book1.setTitle("High-Performance Java Persistence");
		book1.setIsbn("978-9730228236");

		Library library = doInJPA(this::entityManagerFactory, entityManager -> {
			Library _library = entityManager.find(Library.class, 1L);

			_library.getBooks().add(book1);

			return _library;
		});

		assertTrue(library.getBooks().contains(book1));
		//end::entity-pojo-natural-id-equals-hashcode-persist-example[]
	}

	//tag::entity-pojo-natural-id-equals-hashcode-example[]
	@Entity(name = "MyLibrary")
	public static class Library {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "book_id")
		private Set<Book> books = new HashSet<>();

		//Getters and setters are omitted for brevity
	//end::entity-pojo-natural-id-equals-hashcode-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Book> getBooks() {
			return books;
		}
	//tag::entity-pojo-natural-id-equals-hashcode-example[]
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String isbn;

		//Getters and setters are omitted for brevity
	//end::entity-pojo-natural-id-equals-hashcode-example[]

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

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}

		//tag::entity-pojo-natural-id-equals-hashcode-example[]

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Book)) {
				return false;
			}
			Book book = (Book) o;
			return Objects.equals(isbn, book.getIsbn());
		}

		@Override
		public int hashCode() {
			return Objects.hash(isbn);
		}
	}
	//end::entity-pojo-natural-id-equals-hashcode-example[]
}
