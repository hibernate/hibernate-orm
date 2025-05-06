/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SelectDistinctTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class
		};
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person gavinKing = new Person("Gavin", "King");
			Person stephenKing = new Person("Stephen", "King");
			Person vladMihalcea = new Person("Vlad", "Mihalcea");

			gavinKing.addBook(new Book("Hibernate in Action"));
			gavinKing.addBook(new Book("Java Persistence with Hibernate"));

			stephenKing.addBook(new Book("The Green Mile"));

			vladMihalcea.addBook(new Book("High-Performance Java Persistence"));

			entityManager.persist(gavinKing);
			entityManager.persist(stephenKing);
			entityManager.persist(vladMihalcea);
		});
	}

	@Test
	public void testDistinctProjection() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-distinct-projection-query-example[]
			List<String> lastNames = entityManager.createQuery(
				"select distinct p.lastName " +
				"from Person p", String.class)
			.getResultList();
			//end::hql-distinct-projection-query-example[]

			assertTrue(
				lastNames.size() == 2 &&
				lastNames.contains("King") &&
				lastNames.contains("Mihalcea")
			);
		});
	}

	@Entity(name = "Person") @Table(name = "person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		@OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
		private List<Book> books = new ArrayList<>();

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
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

		public List<Book> getBooks() {
			return books;
		}

		public void addBook(Book book) {
			books.add(book);
			book.setAuthor(this);
		}
	}

	@Entity(name = "Book") @Table(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToOne
		private Person author;

		public Book() {
		}

		public Book(String title) {
			this.title = title;
		}

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

		public Person getAuthor() {
			return author;
		}

		public void setAuthor(Person author) {
			this.author = author;
		}
	}
}
