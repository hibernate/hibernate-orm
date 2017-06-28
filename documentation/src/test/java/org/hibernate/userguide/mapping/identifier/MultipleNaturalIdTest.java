/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultipleNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Publisher.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Publisher publisher = new Publisher();
			publisher.setId( 1L );
			publisher.setName( "Amazon" );
			entityManager.persist( publisher );

			Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setProductNumber( "973022823X" );
			book.setPublisher( publisher );

			entityManager.persist( book );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Publisher publisher = entityManager.getReference( Publisher.class, 1L );
			//tag::naturalid-load-access-example[]

			Book book = entityManager
				.unwrap(Session.class)
				.byNaturalId( Book.class )
				.using("productNumber", "973022823X")
				.using("publisher", publisher)
				.load();
			//end::naturalid-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		} );
	}

	//tag::naturalid-multiple-attribute-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String productNumber;

		@NaturalId
		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher publisher;

		//Getters and setters are omitted for brevity
	//end::naturalid-multiple-attribute-mapping-example[]

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

		public String getProductNumber() {
			return productNumber;
		}

		public void setProductNumber(String productNumber) {
			this.productNumber = productNumber;
		}

		public Publisher getPublisher() {
			return publisher;
		}

		public void setPublisher(Publisher publisher) {
			this.publisher = publisher;
		}
	//tag::naturalid-multiple-attribute-mapping-example[]
	}

	@Entity(name = "Publisher")
	public static class Publisher implements Serializable {

		@Id
		private Long id;

		private String name;

		//Getters and setters are omitted for brevity
		//end::naturalid-multiple-attribute-mapping-example[]

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

	//tag::naturalid-multiple-attribute-mapping-example[]

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Publisher publisher = (Publisher) o;
			return Objects.equals( id, publisher.id ) &&
					Objects.equals( name, publisher.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}
	}
	//end::naturalid-multiple-attribute-mapping-example[]
}
