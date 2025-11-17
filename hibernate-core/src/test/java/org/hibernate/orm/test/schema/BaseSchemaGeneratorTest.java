/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

public abstract class BaseSchemaGeneratorTest {
	//tag::schema-generation-domain-model-example[]
	@Entity(name = "Customer")
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

		//end::schema-generation-domain-model-example[]

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
		//tag::schema-generation-domain-model-example[]
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "author")
		private List<Book> books = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::schema-generation-domain-model-example[]

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
		//tag::schema-generation-domain-model-example[]
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

		//end::schema-generation-domain-model-example[]

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
		//tag::schema-generation-domain-model-example[]
	}
	//end::schema-generation-domain-model-example[]
}
