/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = AttributeOverridingTest.Book.class
)
@SessionFactory
public class AttributeOverridingTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Publisher ebookPublisher = new Publisher();
					ebookPublisher.setName( "eprint" );

					Publisher paperPublisher = new Publisher();
					paperPublisher.setName( "paperbooks" );

					Book book = new Book();
					book.setTitle( "Hibernate" );
					book.setAuthor( "Steve" );
					book.setEbookPublisher( ebookPublisher );
					book.setPaperBackPublisher( paperPublisher );

					session.persist( book );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "from Book" ).list();
				}
		);
	}


	@Entity(name = "Book")
	@AttributeOverrides({
			@AttributeOverride(
					name = "ebookPublisher.name",
					column = @Column(name = "ebook_publisher_name")
			),
			@AttributeOverride(
					name = "paperBackPublisher.name",
					column = @Column(name = "paper_back_publisher_name")
			)
	})
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher ebookPublisher;

		private Publisher paperBackPublisher;

		//Getters and setters are omitted for brevity

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

		public Publisher getEbookPublisher() {
			return ebookPublisher;
		}

		public void setEbookPublisher(Publisher ebookPublisher) {
			this.ebookPublisher = ebookPublisher;
		}

		public Publisher getPaperBackPublisher() {
			return paperBackPublisher;
		}

		public void setPaperBackPublisher(Publisher paperBackPublisher) {
			this.paperBackPublisher = paperBackPublisher;
		}
	}

	@Embeddable
	public static class Publisher {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
