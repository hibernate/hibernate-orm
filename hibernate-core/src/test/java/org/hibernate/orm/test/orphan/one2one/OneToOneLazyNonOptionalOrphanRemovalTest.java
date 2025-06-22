/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Cranford
 */
@JiraKey("HHH-9663")
@DomainModel(
		annotatedClasses = {
				OneToOneLazyNonOptionalOrphanRemovalTest.Car.class,
				OneToOneLazyNonOptionalOrphanRemovalTest.PaintColor.class,
				OneToOneLazyNonOptionalOrphanRemovalTest.Engine.class
		}
)
@SessionFactory
public class OneToOneLazyNonOptionalOrphanRemovalTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneLazyNonOptionalOrphanRemoval(SessionFactoryScope scope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Initialize the data
		scope.inTransaction( session -> {
			final PaintColor color = new PaintColor( 1, "Red" );
			final Engine engine1 = new Engine( 1, 275 );
			final Engine engine2 = new Engine( 2, 295 );
			final Car car = new Car( 1, engine1, color );

			session.persist( engine1 );
			session.persist( engine2 );
			session.persist( color );
			session.persist( car );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for unidirectional relationship
		scope.inTransaction( session -> {
			final Car car = session.find( Car.class, 1 );
			final Engine engine = session.find( Engine.class, 2 );
			car.setEngine( engine );
			session.merge( car );
		} );

		scope.inTransaction( session -> {
			final Car car = session.find( Car.class, 1 );
			assertNotNull( car.getEngine() );

			final Engine engine = session.find( Engine.class, 1 );
			assertNull( engine );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for bidirectional relationship
		scope.inTransaction( session -> {
			final PaintColor color = new PaintColor( 2, "Blue" );
			final Car car = session.find( Car.class, 1 );
			car.setPaintColor( color );
			session.persist( color );
			session.merge( car );
		} );

		scope.inTransaction( session -> {
			final Car car = session.find( Car.class, 1 );
			assertNotNull( car.getPaintColor() );

			final PaintColor color = session.find( PaintColor.class, 1 );
			assertNull( color );
		} );
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		private Integer id;

		private String model;

		// represents a bidirectional one-to-one
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY, optional = false)
		private PaintColor paintColor;

		// represents a unidirectional one-to-one
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY, optional = false)
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
