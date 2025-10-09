/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = OptimisticLockingInstantTest.Person.class)
public class OptimisticLockingInstantTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(EntityManagerFactoryScope factoryScope) {
		var _person = factoryScope.fromTransaction( entityManager -> {
			var person = new Person();
			person.setName("John Doe");
			entityManager.persist(person);
			return person;
		} );

		factoryScope.inTransaction(entityManager -> {
			var person = entityManager.find(Person.class, _person.getId());
			person.setName(person.getName().toUpperCase());
		} );
	}

	//tag::locking-optimistic-entity-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`name`")
		private String name;

		//tag::locking-optimistic-version-number-example[]
		@Version
		private Instant version;
		//end::locking-optimistic-version-number-example[]

		//Getters and setters are omitted for brevity

		//end::locking-optimistic-entity-mapping-example[]
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

		public Instant getVersion() {
			return version;
		}
	//tag::locking-optimistic-entity-mapping-example[]
	}
	//end::locking-optimistic-entity-mapping-example[]
}
