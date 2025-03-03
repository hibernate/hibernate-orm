/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-9663")
@Jpa(
		annotatedClasses = {
				OneToOneLazyOrphanRemovalTest.Car.class,
				OneToOneLazyOrphanRemovalTest.PaintColor.class,
				OneToOneLazyOrphanRemovalTest.Engine.class
		}
)
public class OneToOneLazyOrphanRemovalTest {

	@Test
	public void testOneToOneLazyOrphanRemoval(EntityManagerFactoryScope scope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Initialize the data
		scope.inTransaction( entityManager -> {
			final PaintColor color = new PaintColor( 1, "Red" );
			final Engine engine = new Engine( 1, 275 );
			final Car car = new Car( 1, engine, color );

			entityManager.persist( engine );
			entityManager.persist( color );
			entityManager.persist( car );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for unidirectional relationship
		scope.inTransaction( entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			car.setEngine( null );
			entityManager.merge( car );
		} );

		scope.inTransaction( entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			assertNull( car.getEngine() );

			final Engine engine = entityManager.find( Engine.class, 1 );
			assertNull( engine );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for bidirectional relationship
		scope.inTransaction( entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			car.setPaintColor( null );
			entityManager.merge( car );
		} );

		scope.inTransaction( entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			assertNull( car.getPaintColor() );

			final PaintColor color = entityManager.find( PaintColor.class, 1 );
			assertNull( color );
		} );
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		private Integer id;

		// represents a bidirectional one-to-one
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		private PaintColor paintColor;

		// represents a unidirectional one-to-one
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		private Engine engine;

		Car() {
			// Required by JPA
		}

		Car(Integer id, Engine engine, PaintColor paintColor) {
			this.id = id;
			this.engine = engine;
			this.paintColor = paintColor;
			paintColor.setCar( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public PaintColor getPaintColor() {
			return paintColor;
		}

		public void setPaintColor(PaintColor paintColor) {
			this.paintColor = paintColor;
		}

		public Engine getEngine() {
			return engine;
		}

		public void setEngine(Engine engine) {
			this.engine = engine;
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

	@Entity(name = "PaintColor")
	public static class PaintColor {
		@Id
		private Integer id;
		private String color;

		@OneToOne(mappedBy = "paintColor")
		private Car car;

		PaintColor() {
			// Required by JPA
		}

		PaintColor(Integer id, String color) {
			this.id = id;
			this.color = color;
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

		public Car getCar() {
			return car;
		}

		public void setCar(Car car) {
			this.car = car;
		}
	}
}
