/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class BytecodeEnhancementTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class,
			Customer.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::BytecodeEnhancement-dirty-tracking-bidirectional-incorrect-usage-example[]
			Person person = new Person();
			person.setName("John Doe");

			Book book = new Book();
			person.getBooks().add(book);
			try {
				book.getAuthor().getName();
			}
			catch (NullPointerException expected) {
				// This blows up (NPE) in normal Java usage
			}
			//end::BytecodeEnhancement-dirty-tracking-bidirectional-incorrect-usage-example[]
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::BytecodeEnhancement-dirty-tracking-bidirectional-correct-usage-example[]
			Person person = new Person();
			person.setName("John Doe");

			Book book = new Book();
			person.getBooks().add(book);
			book.setAuthor(person);

			book.getAuthor().getName();
			//end::BytecodeEnhancement-dirty-tracking-bidirectional-correct-usage-example[]
		});
	}

	//tag::BytecodeEnhancement-lazy-loading-example[]
	@Entity
	public class Customer {

		@Id
		private Integer id;

		private String name;

		@Basic(fetch = FetchType.LAZY)
		private UUID accountsPayableXrefId;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("lobs")
		private Blob image;

		//Getters and setters are omitted for brevity

	//end::BytecodeEnhancement-lazy-loading-example[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public UUID getAccountsPayableXrefId() {
			return accountsPayableXrefId;
		}

		public void setAccountsPayableXrefId(UUID accountsPayableXrefId) {
			this.accountsPayableXrefId = accountsPayableXrefId;
		}

		public Blob getImage() {
			return image;
		}

		public void setImage(Blob image) {
			this.image = image;
		}
	//tag::BytecodeEnhancement-lazy-loading-example[]
	}
	//end::BytecodeEnhancement-lazy-loading-example[]

	//tag::BytecodeEnhancement-dirty-tracking-bidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "author")
		private List<Book> books = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::BytecodeEnhancement-dirty-tracking-bidirectional-example[]

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

		public List<Book> getBooks() {
			return books;
		}
	//tag::BytecodeEnhancement-dirty-tracking-bidirectional-example[]
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@NaturalId
		private String isbn;

		@ManyToOne
		private Person author;

		//Getters and setters are omitted for brevity

	//end::BytecodeEnhancement-dirty-tracking-bidirectional-example[]

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

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	//tag::BytecodeEnhancement-dirty-tracking-bidirectional-example[]
	}
	//end::BytecodeEnhancement-dirty-tracking-bidirectional-example[]
}
