/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingInstantTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		Person _person = doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setName("John Doe");
			entityManager.persist(person);

			return person;
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(Person.class, _person.getId());
			person.setName(person.getName().toUpperCase());
		});
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
