/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.schema;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( PostgreSQL81Dialect.class )
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
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setId( 0L );
			book.setTitle( "Hibernate in Action" );
			book.setPrice( 49.99d );

			entityManager.persist( book );
		} );
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				//tag::schema-generation-database-checks-persist-example[]
				Book book = new Book();
				book.setId( 1L );
				book.setPrice( 49.99d );
				book.setTitle( "High-Performance Java Persistence" );
				book.setIsbn( "11-11-2016" );

				entityManager.persist( book );
				//end::schema-generation-database-checks-persist-example[]
			} );
			fail("Should fail because the ISBN is not of the right length!");
		}
		catch ( PersistenceException e ) {
			assertEquals( ConstraintViolationException.class, e.getCause().getCause().getClass() );
		}
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Person person = new Person();
				person.setId( 1L );
				person.setName( "John Doe" );
				person.setCode( 0L );

				entityManager.persist( person );
			} );
			fail("Should fail because the code is 0!");
		}
		catch ( PersistenceException e ) {
			assertEquals( ConstraintViolationException.class, e.getCause().getCause().getClass() );
		}
	}

	@Entity(name = "Person")
	@Check( constraints = "code > 0" )
	public static class Person {

		@Id
		private Long id;

		private String name;

		// This one does not work! Only the entity-level annotation works.
		// @Check( constraints = "code > 0" )
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
	@Check( constraints = "CASE WHEN isbn IS NOT NULL THEN LENGTH(isbn) = 13 ELSE true END")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@NaturalId
		private String isbn;

		private Double price;

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
