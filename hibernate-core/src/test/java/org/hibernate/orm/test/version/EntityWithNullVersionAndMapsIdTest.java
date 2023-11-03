package org.hibernate.orm.test.version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

@DomainModel(
		annotatedClasses = {
				EntityWithNullVersionAndMapsIdTest.Person.class,
				EntityWithNullVersionAndMapsIdTest.Address.class,
		}
)
@SessionFactory
@JiraKey("HHH-17380")
public class EntityWithNullVersionAndMapsIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Person" );
					session.createMutationQuery( "delete from Address" );

				}
		);
	}

	@Test
	public void testPersistEntityWithMapsId(SessionFactoryScope scope) {
		Address address = new Address( "Alex" );
		scope.inTransaction(
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
