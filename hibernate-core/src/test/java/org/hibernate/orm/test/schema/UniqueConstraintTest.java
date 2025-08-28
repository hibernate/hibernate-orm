/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */

public class UniqueConstraintTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Author.class,
		};
	}

	@Test
	public void test() {
		//tag::schema-generation-columns-unique-constraint-persist-example[]
		Author _author = doInJPA(this::entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setFirstName("Vlad");
			author.setLastName("Mihalcea");
			entityManager.persist(author);

			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor(author);
			entityManager.persist(book);

			return author;
		});

		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				Book book = new Book();
				book.setTitle("High-Performance Java Persistence");
				book.setAuthor(_author);
				entityManager.persist(book);
			});
		}
		catch (Exception expected) {
			assertNotNull(ExceptionUtil.findCause(expected, ConstraintViolationException.class));
		}
		//end::schema-generation-columns-unique-constraint-persist-example[]
	}

	//tag::schema-generation-columns-unique-constraint-mapping-example[]
	@Entity
	@Table(
		name = "book",
		uniqueConstraints =  @UniqueConstraint(
			name = "uk_book_title_author",
			columnNames = {
				"title",
				"author_id"
			}
	)
)
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(
			name = "author_id",
			foreignKey = @ForeignKey(name = "fk_book_author_id")
	)
		private Author author;

		//Getter and setters omitted for brevity
	//end::schema-generation-columns-unique-constraint-mapping-example[]

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

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}

	//tag::schema-generation-columns-unique-constraint-mapping-example[]
	}

	@Entity
	@Table(name = "author")
	public static class Author {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		//Getter and setters omitted for brevity
	//end::schema-generation-columns-unique-constraint-mapping-example[]

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

	//tag::schema-generation-columns-unique-constraint-mapping-example[]
	}
	//end::schema-generation-columns-unique-constraint-mapping-example[]
}
