/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;

import jakarta.persistence.SecondaryTable;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import java.time.LocalDate;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(H2Dialect.class)
public class CheckTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setId(0L);
			book.setTitle("Hibernate in Action");
			book.setPrice(49.99d);

			entityManager.persist(book);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Book book = entityManager.find(Book.class, 0L);
			assertEquals( 1, book.edition );
			assertEquals( 2, book.nextEdition );
		});
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				//tag::schema-generation-database-checks-persist-example[]
				Book book = new Book();
				book.setId(1L);
				book.setPrice(49.99d);
				book.setTitle("High-Performance Java Persistence");
				book.setIsbn("11-11-2016");

				entityManager.persist(book);
				//end::schema-generation-database-checks-persist-example[]
			});
			fail("Should fail because the ISBN is not of the right length!");
		}
		catch (PersistenceException e) {
			assertEquals(ConstraintViolationException.class, e.getCause().getClass());
		}
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				Person person = new Person();
				person.setId(1L);
				person.setName("John Doe");
				person.setCode(0L);

				entityManager.persist(person);
			});
			fail("Should fail because the code is 0!");
		}
		catch (PersistenceException e) {
			assertEquals(ConstraintViolationException.class, e.getCause().getClass());
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Check(constraints = "code > 0")
		private Long code;

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

		public Long getCode() {
			return code;
		}

		public void setCode(Long code) {
			this.code = code;
		}
	}

	//tag::schema-generation-database-checks-example[]
	@Entity(name = "Book")
	@Check(name = "ValidIsbn", constraints = "CASE WHEN isbn IS NOT NULL THEN LENGTH(isbn) = 13 ELSE true END")
	@SecondaryTable(name = "BookEdition")
	@Check(name = "PositiveEdition", constraints = "edition > 0")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@NaturalId
		private String isbn;

		private Double price;

		@Column(table = "BookEdition")
		private int edition = 1;

		@Formula("edition + 1")
		private int nextEdition = 2;

		@Column(table = "BookEdition")
		private LocalDate editionDate;

		//Getters and setters omitted for brevity

	//end::schema-generation-database-checks-example[]
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

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}

		public Double getPrice() {
			return price;
		}

		public void setPrice(Double price) {
			this.price = price;
		}
	//tag::schema-generation-database-checks-example[]
	}
	//end::schema-generation-database-checks-example[]
}
