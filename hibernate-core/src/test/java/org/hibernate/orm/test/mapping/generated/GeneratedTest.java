/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class)
@Jpa(
		annotatedClasses = GeneratedTest.Person.class
)
public class GeneratedTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::mapping-generated-Generated-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.setFirstName("John");
			person.setMiddleName1("Flávio");
			person.setMiddleName2("André");
			person.setMiddleName3("Frederico");
			person.setMiddleName4("Rúben");
			person.setMiddleName5("Artur");
			person.setLastName("Doe");

			entityManager.persist(person);
			entityManager.flush();

			assertEquals("John Flávio André Frederico Rúben Artur Doe", person.getFullName());
			//end::mapping-generated-Generated-persist-example[]
		});
		scope.inTransaction( entityManager -> {
			//tag::mapping-generated-Generated-update-example[]
			Person person = entityManager.find(Person.class, 1L);
			person.setLastName("Doe Jr");

			entityManager.flush();
			assertEquals("John Flávio André Frederico Rúben Artur Doe Jr", person.getFullName());
			//end::mapping-generated-Generated-update-example[]
		});
	}

	//tag::mapping-generated-provided-generated[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String middleName1;

		private String middleName2;

		private String middleName3;

		private String middleName4;

		private String middleName5;

		@Generated(event = {INSERT,UPDATE})
		@Column(columnDefinition =
			"AS CONCAT(" +
			"	COALESCE(firstName, ''), " +
			"	COALESCE(' ' + middleName1, ''), " +
			"	COALESCE(' ' + middleName2, ''), " +
			"	COALESCE(' ' + middleName3, ''), " +
			"	COALESCE(' ' + middleName4, ''), " +
			"	COALESCE(' ' + middleName5, ''), " +
			"	COALESCE(' ' + lastName, '') " +
			")")
		private String fullName;

	//end::mapping-generated-provided-generated[]
		public Person() {}

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

		public String getMiddleName1() {
			return middleName1;
		}

		public void setMiddleName1(String middleName1) {
			this.middleName1 = middleName1;
		}

		public String getMiddleName2() {
			return middleName2;
		}

		public void setMiddleName2(String middleName2) {
			this.middleName2 = middleName2;
		}

		public String getMiddleName3() {
			return middleName3;
		}

		public void setMiddleName3(String middleName3) {
			this.middleName3 = middleName3;
		}

		public String getMiddleName4() {
			return middleName4;
		}

		public void setMiddleName4(String middleName4) {
			this.middleName4 = middleName4;
		}

		public String getMiddleName5() {
			return middleName5;
		}

		public void setMiddleName5(String middleName5) {
			this.middleName5 = middleName5;
		}

		public String getFullName() {
			return fullName;
		}
	//tag::mapping-generated-provided-generated[]
	}
	//end::mapping-generated-provided-generated[]
}
