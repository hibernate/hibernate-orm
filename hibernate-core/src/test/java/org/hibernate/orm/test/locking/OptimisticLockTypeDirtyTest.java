/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = OptimisticLockTypeDirtyTest.Person.class)
public class OptimisticLockTypeDirtyTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			var person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setCountry("US");
			person.setCity("New York");
			person.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			entityManager.persist(person);
		});

		factoryScope.inTransaction( entityManager -> {
			//tag::locking-optimistic-lock-type-dirty-update-example[]
			var person = entityManager.find(Person.class, 1L);
			person.setCity("Washington D.C.");
			//end::locking-optimistic-lock-type-dirty-update-example[]
		});
	}

	//tag::locking-optimistic-lock-type-dirty-example[]
	@Entity(name = "Person")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Person {

		@Id
		private Long id;

		@Column(name = "`name`")
		private String name;

		private String country;

		private String city;

		@Column(name = "created_on")
		private Timestamp createdOn;

		//Getters and setters are omitted for brevity
	//end::locking-optimistic-lock-type-dirty-example[]

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

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public Timestamp getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Timestamp createdOn) {
			this.createdOn = createdOn;
		}
	//tag::locking-optimistic-lock-type-dirty-example[]
	}
	//end::locking-optimistic-lock-type-dirty-example[]
}
