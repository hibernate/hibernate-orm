/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = ColumnDefaultTest.Person.class)
public class ColumnDefaultTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(EntityManagerFactoryScope factories) {
		//tag::schema-generation-column-default-value-persist-example[]
		factories.inTransaction( entityManager -> {
			var person = new Person();
			person.setId(1L);
			entityManager.persist(person);
		});
		factories.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			assertEquals( "N/A", person.getName() );
			assertEquals( Long.valueOf(-1L), person.getClientId() );
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
