/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = FlushOrderTest.Person.class)
@SessionFactory
public class FlushOrderTest {
	private final Logger log = Logger.getLogger( FlushOrderTest.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testOrder(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Person person = new Person("John Doe");
			person.id = 1L;
			entityManager.persist(person);
		});

		factoryScope.inTransaction( entityManager -> {
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
