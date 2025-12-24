/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Jpa(
		annotatedClasses = {
				UnownedOneToOneJoinedInheritanceTest.Owner.class,
				UnownedOneToOneJoinedInheritanceTest.Vehicle.class,
				UnownedOneToOneJoinedInheritanceTest.Car.class,
				UnownedOneToOneJoinedInheritanceTest.Person.class
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-9499")
public class UnownedOneToOneJoinedInheritanceTest {

	@Test
	public void testModel(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Car car = new Car();
			car.setId( 1L );

			Person person = new Person();
			person.setId( 2L );
			person.setVehicle( car );

			entityManager.persist( car );
			entityManager.persist( person );
		} );

		scope.inTransaction( entityManager -> {
			Person person = entityManager.find( Person.class, 2L );
			assertNotNull( person, "Person should be found" );
			assertNotNull( person.getVehicle(), "Vehicle reference should not be null" );
			assertEquals( 1L, person.getVehicle().getId(), "Vehicle should have correct id" );

			Car car = entityManager.find( Car.class, 1L );
			assertNotNull( car, "Car should be found" );
			assertNotNull( car.getOwner(), "Owner reference should not be null" );
			assertEquals( 2L, car.getOwner().getId(), "Owner should have correct id" );

			assertEquals( person, car.getOwner(), "Owner should be the same Person" );
		} );
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Owner {
		@Id
		private Long id;

		@OneToOne
		private Vehicle vehicle;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle vehicle) {
			this.vehicle = vehicle;
		}
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Vehicle {
		@Id
		private Long id;

		@OneToOne(mappedBy = "vehicle")
		private Owner owner;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Owner getOwner() {
			return owner;
		}

		public void setOwner(Owner owner) {
			this.owner = owner;
		}
	}

	@Entity
	public static class Car extends Vehicle {
		@OneToOne(mappedBy = "vehicle")
		private Person person;

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

	@Entity
	public static class Person extends Owner {
	}
}
