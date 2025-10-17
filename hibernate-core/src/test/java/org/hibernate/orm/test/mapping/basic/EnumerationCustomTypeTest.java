/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.annotations.Type;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {EnumerationCustomTypeTest.Person.class} )
public class EnumerationCustomTypeTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setGender(Gender.MALE);
			entityManager.persist(person);
		});
	}

	//tag::basic-enums-custom-type-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Type(GenderType.class)
		@Column(length = 6)
		public Gender gender;

		//Getters and setters are omitted for brevity

	//end::basic-enums-custom-type-example[]
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

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}
	//tag::basic-enums-custom-type-example[]
	}
	//end::basic-enums-custom-type-example[]
}
