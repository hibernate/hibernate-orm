/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = VersionSourceTest.Person.class )
@SessionFactory
public class VersionSourceTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::locking-optimistic-version-timestamp-source-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.setFirstName("John");
			person.setLastName("Doe");
			assertNull( person.getVersion() );

			entityManager.persist(person);
			assertNotNull( person.getVersion() );
			//end::locking-optimistic-version-timestamp-source-persist-example[]
		});
		sleep();
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("Jane");
		});
		sleep();
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("John");
		});
	}

	private static void sleep() {
		try {
			Thread.sleep(300);
		}
		catch (InterruptedException ignored) {
		}
	}

	//tag::locking-optimistic-version-timestamp-source-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Version @CurrentTimestamp
		private LocalDateTime version;
	//end::locking-optimistic-version-timestamp-source-mapping-example[]

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

		public LocalDateTime getVersion() {
			return version;
		}
	}
}
