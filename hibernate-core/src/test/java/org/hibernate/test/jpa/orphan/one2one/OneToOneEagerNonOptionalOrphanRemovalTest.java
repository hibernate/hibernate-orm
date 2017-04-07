/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.orphan.one2one;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-9663" )
public class OneToOneEagerNonOptionalOrphanRemovalTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Car.class, PaintColor.class, Engine.class };
	}

	@Test
	public void testOneToOneLazyNonOptionalOrphanRemoval() {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Initialize the data
		doInJPA( this::entityManagerFactory, entityManager -> {
			final PaintColor color = new PaintColor( 1, "Red" );
			final Engine engine1 = new Engine( 1, 275 );
			final Engine engine2 = new Engine( 2, 295 );
			final Car car = new Car( 1, engine1, color );

			entityManager.persist( engine1 );
			entityManager.persist( engine2 );
			entityManager.persist( color );
			entityManager.persist( car );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for unidirectional relationship
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			final Engine engine = entityManager.find( Engine.class, 2 );
			car.setEngine( engine );
			entityManager.merge( car );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			assertNotNull( car.getEngine() );

			final Engine engine = entityManager.find( Engine.class, 1 );
			assertNull( engine );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for bidirectional relationship
		doInJPA( this::entityManagerFactory, entityManager -> {
			final PaintColor color = new PaintColor( 2, "Blue" );
			final Car car = entityManager.find( Car.class, 1 );
			car.setPaintColor( color );
			entityManager.persist( color );
			entityManager.merge( car );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final Car car = entityManager.find( Car.class, 1 );
			assertNotNull( car.getPaintColor() );

			final PaintColor color = entityManager.find( PaintColor.class, 1 );
			assertNull( color );
		} );
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		private Integer id;

		// represents a bidirectional one-to-one
		@OneToOne(orphanRemoval = true, optional = false)
		private PaintColor paintColor;

		// represents a unidirectional one-to-one
		@OneToOne(orphanRemoval = true, optional = false)
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
