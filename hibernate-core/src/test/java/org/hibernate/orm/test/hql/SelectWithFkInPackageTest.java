/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Jpa(
		annotatedClasses = {
				SelectWithFkInPackageTest.Person.class,
				SelectWithFkInPackageTest.Book.class
		}
)
class SelectWithFkInPackageTest {

	@BeforeAll
	void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Person( "name" ) );
			entityManager.persist( new Book( "title" ) );
		} );
	}

	@AfterAll
	void tearDown(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from org.fk.entity.Person" );
			entityManager.createQuery( "delete from org.right.entity.Book" );
		} );
	}

	@Test
	void selectWithFk(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Person> people = entityManager.createQuery( "from org.fk.entity.Person", Person.class )
					.getResultList();

			assertThat( people ).hasSize( 1 );
		} );
	}

	@Test
	void selectNoFk(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Book> books = entityManager.createQuery( "from org.right.entity.Book", Book.class )
					.getResultList();

			assertThat( books ).hasSize( 1 );
		} );
	}

	@Entity(name = "org.fk.entity.Person")
	@Table(name = "person")
	public static class Person {

		@Id
		@GeneratedValue
		public Long id;

		@Column
		public String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}
	}

	@Entity(name = "org.right.entity.Book")
	@Table(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		public Long id;

		@Column
		public String title;

		public Book() {
		}

		public Book(String title) {
			this.title = title;
		}
	}

}
