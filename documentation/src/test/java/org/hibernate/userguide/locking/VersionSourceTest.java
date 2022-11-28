/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class VersionSourceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::locking-optimistic-version-timestamp-source-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.setFirstName("John");
			person.setLastName("Doe");
			assertNull(person.getVersion());

			entityManager.persist(person);
			assertNotNull(person.getVersion());
			//end::locking-optimistic-version-timestamp-source-persist-example[]
		});
		sleep();
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("Jane");
		});
		sleep();
		doInJPA(this::entityManagerFactory, entityManager -> {
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
