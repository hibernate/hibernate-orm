/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(annotatedClasses = NaturalIdClassTest.Person.class)
public class NaturalIdClassTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var entity = new Person();
			entity.firstName = "Gavin";
			entity.lastName = "King";
			em.persist( entity );
		} );
		scope.inTransaction( em -> {
			var entity = em.find( Person.class, new Name("Gavin", "King") );
			assertNotNull( entity );
		} );
	}

	static class Name {
		String firstName;
		String lastName;
		Name(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	static class PersonId {
		String id;
		String country;
		PersonId(String id, String country) {
			this.id = id;
			this.country = country;
		}
		PersonId() {}
	}

	@Entity
	@IdClass(PersonId.class)
	@NaturalIdClass(Name.class)
	static class Person {
		@Id String id;
		@Id String country;
		@NaturalId String firstName;
		@NaturalId String lastName;
	}
}
