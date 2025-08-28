/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ColumnDefaultTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Test
	public void test() {
		//tag::schema-generation-column-default-value-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId(1L);
			entityManager.persist(person);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			assertEquals("N/A", person.getName());
			assertEquals(Long.valueOf(-1L), person.getClientId());
		});
		//end::schema-generation-column-default-value-persist-example[]
	}

	//tag::schema-generation-column-default-value-mapping-example[]
	@Entity(name = "Person")
	@DynamicInsert
	public static class Person {

		@Id
		private Long id;

		@ColumnDefault("'N/A'")
		private String name;

		@ColumnDefault("-1")
		private Long clientId;

		//Getter and setters omitted for brevity

	//end::schema-generation-column-default-value-mapping-example[]

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

		public Long getClientId() {
			return clientId;
		}

		public void setClientId(Long clientId) {
			this.clientId = clientId;
		}
	//tag::schema-generation-column-default-value-mapping-example[]
	}
	//end::schema-generation-column-default-value-mapping-example[]
}
