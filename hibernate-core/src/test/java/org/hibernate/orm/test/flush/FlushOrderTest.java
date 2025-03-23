/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class FlushOrderTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Person.class
		};
	}

	@Test
	public void testOrder() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery("delete from Person").executeUpdate();
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person("John Doe");
			person.id = 1L;
			entityManager.persist(person);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			log.info("testFlushSQL");
			//tag::flushing-order-example[]
			Person person = entityManager.find(Person.class, 1L);
			entityManager.remove(person);

			Person newPerson = new Person();
			newPerson.setId(2L);
			newPerson.setName("John Doe");
			entityManager.persist(newPerson);
			//end::flushing-order-example[]
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

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
	}
}
