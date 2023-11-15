package org.hibernate.test.version;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-17380")
public class EntityWithNullVersionAndMapsIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Address.class
		};
	}

	@Test
	public void testPersistEntityWithMapsId() {
		Address address = new Address( "Alex" );
		inTransaction(
				session -> {
					session.persist( address );
					Person person = new Person( address );
					session.persist( person );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private String name;

		@Version
		private Long version;

		private String description;

		@MapsId
		@OneToOne
		@JoinColumn(name = "name")
		private Address address;

		public Person() {
		}

		public Person(Address address) {
			this.name = address.getName();
			this.address = address;
		}

		public String getName() {
			return name;
		}

		public Address getAddress() {
			return address;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		private String name;

		@Version
		private Long version;

		private String description;

		public Address() {
		}

		public Address(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
