/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone.embedded;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Similar test as ../OneToOneLazyOrphanRemovalTest,
 * shows that orphanRemoval = true is not removing the orphan of OneToOne relations in Embedded objects
 * for unidirectional relationship.
 *
 * @JiraKey( value = "HHH-9663" )
 */
@Jpa(
		annotatedClasses = {
				OneToOneLazyOrphanRemovalInEmbeddedEntityTest.RaceDriver.class,
				OneToOneLazyOrphanRemovalInEmbeddedEntityTest.Car.class,
				OneToOneLazyOrphanRemovalInEmbeddedEntityTest.Engine.class
		}
)
public class OneToOneLazyOrphanRemovalInEmbeddedEntityTest {

	@Test
	public void testOneToOneLazyOrphanRemoval(EntityManagerFactoryScope scope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Initialize the data
		scope.inTransaction( entityManager -> {
			final Engine engine = new Engine( 1, 275 );
			final Car car = new Car( 1, engine, "red" );
			final RaceDriver raceDriver = new RaceDriver( 1, car );
			entityManager.persist( engine );
			entityManager.persist( raceDriver );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//set car engine to null, orphanRemoval = true should trigger deletion for engine entity
		scope.inTransaction( entityManager -> {
			final RaceDriver raceDriver = entityManager.find( RaceDriver.class, 1 );
			final Car car = raceDriver.getCar();

			//check, that at the moment the engine is orphan
			assertNotNull( car.getEngine() );

			car.setEngine( null );
			entityManager.merge( raceDriver );

			final RaceDriver raceDriver2 = entityManager.find( RaceDriver.class, 1 );
			assertNotNull( raceDriver2.car );
		} );

		//check, that the engine is deleted:
		scope.inTransaction( entityManager -> {
			final RaceDriver raceDriver = entityManager.find( RaceDriver.class, 1 );
			final Car car = raceDriver.getCar();
			assertNull( car.getEngine() );

			final Engine engine = entityManager.find( Engine.class, 1 );
			assertNull( engine );
		} );
	}

	@Entity(name = "RaceDriver")
	public static class RaceDriver {

		@Id
		private Integer id;

		@Embedded
		private Car car;

		RaceDriver() {
			// Required by JPA
		}

		RaceDriver(Integer id, Car car) {
			this.id = id;
			this.car = car;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Car getCar() {
			return car;
		}

		public void setCar(Car car) {
			this.car = car;
		}
	}

	@Embeddable
	public static class Car {

		@Id
		private Integer id;

		// represents a unidirectional one-to-one
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		private Engine engine;

		@Column
		private String color;

		Car() {
			// Required by JPA
		}

		Car(Integer id, Engine engine, String color) {
			this.id = id;
			this.engine = engine;
			this.color = color;
		}

		public Engine getEngine() {
			return engine;
		}

		public void setEngine(Engine engine) {
			this.engine = engine;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}
	}

	@Entity(name = "Engine")
	public static class Engine {
		@Id
		private Integer id;
		private Integer horsePower;

		Engine() {
			// Required by JPA
		}

		Engine(Integer id, int horsePower) {
			this.id = id;
			this.horsePower = horsePower;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getHorsePower() {
			return horsePower;
		}

		public void setHorsePower(Integer horsePower) {
			this.horsePower = horsePower;
		}
	}
}
