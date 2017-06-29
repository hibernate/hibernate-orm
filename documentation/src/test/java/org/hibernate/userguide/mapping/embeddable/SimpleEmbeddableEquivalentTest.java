/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.embeddable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class SimpleEmbeddableEquivalentTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( this::entityManagerFactory, entityManager -> {

			Book book = new Book();
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setPublisherName("Amazon");
			book.setPublisherCountry("USA");

			entityManager.persist( book );
		} );
	}

	//tag::embeddable-type-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		@Column(name = "publisher_name")
		private String publisherName;

		@Column(name = "publisher_country")
		private String publisherCountry;

		//Getters and setters are omitted for brevity
	//end::embeddable-type-mapping-example[]

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

		public String getPublisherName() {
			return publisherName;
		}

		public void setPublisherName(String publisherName) {
			this.publisherName = publisherName;
		}

		public String getPublisherCountry() {
			return publisherCountry;
		}

		public void setPublisherCountry(String publisherCountry) {
			this.publisherCountry = publisherCountry;
		}


		//tag::embeddable-type-mapping-example[]
	}
	//end::embeddable-type-mapping-example[]
}
