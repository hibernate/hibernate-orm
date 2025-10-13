/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				DefaultEntityListenerTest.Person.class,
				DefaultEntityListenerTest.Book.class,
				DefaultEntityListenerTest.Publisher.class
		},
		xmlMappings = "org/hibernate/orm/test/events/DefaultEntityListener-orm.xml"
)
@SessionFactory
public class DefaultEntityListenerTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::events-default-listener-persist-example[]
			Person author = new Person();
			author.setId( 1L );
			author.setName( "Vlad Mihalcea" );

			entityManager.persist( author );

			Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( author );

			entityManager.persist( book );
			//end::events-default-listener-persist-example[]
		} );

		scope.inTransaction( entityManager -> {
			//tag::events-default-listener-update-example[]
			Person author = entityManager.find( Person.class, 1L );
			author.setName( "Vlad-Alexandru Mihalcea" );

			Book book = entityManager.find( Book.class, 1L );
			book.setTitle( "High-Performance Java Persistence 2nd Edition" );
			//end::events-default-listener-update-example[]
		} );
	}

	@Test
	public void testExclude(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::events-exclude-default-listener-persist-example[]
			Publisher publisher = new Publisher();
			publisher.setId( 1L );
			publisher.setName( "Amazon" );

			entityManager.persist( publisher );
			//end::events-exclude-default-listener-persist-example[]
		} );
		scope.inTransaction( entityManager -> {
			Publisher publisher = entityManager.find( Publisher.class, 1L );
			assertNull( publisher.getCreatedOn() );
		} );
	}

	//tag::events-default-listener-mapping-example[]
	@Entity(name = "Person")
	public static class Person extends BaseEntity {

		@Id
		private Long id;

		private String name;

		//Getters and setters omitted for brevity
		//end::events-default-listener-mapping-example[]

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
		//tag::events-default-listener-mapping-example[]
	}

	@Entity(name = "Book")
	public static class Book extends BaseEntity {

		@Id
		private Long id;

		private String title;

		@ManyToOne
		private Person author;

		//Getters and setters omitted for brevity
		//end::events-default-listener-mapping-example[]

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
		//tag::events-default-listener-mapping-example[]
	}
	//end::events-default-listener-mapping-example[]

	//tag::events-exclude-default-listener-mapping-example[]
	@Entity(name = "Publisher")
	@ExcludeDefaultListeners
	@ExcludeSuperclassListeners
	public static class Publisher extends BaseEntity {

		@Id
		private Long id;

		private String name;

		//Getters and setters omitted for brevity
		//end::events-exclude-default-listener-mapping-example[]

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

		//tag::events-exclude-default-listener-mapping-example[]
	}
	//end::events-exclude-default-listener-mapping-example[]
}
