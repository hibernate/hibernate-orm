/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Target;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TargetTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			City.class,
		};
	}

	@Test
	public void testLifecycle() {
		//tag::embeddable-Target-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = new City();
			cluj.setName("Cluj");
			cluj.setCoordinates(new GPS(46.77120, 23.62360));

			entityManager.persist(cluj);
		});
		//end::embeddable-Target-persist-example[]


		//tag::embeddable-Target-fetching-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = entityManager.find(City.class, 1L);

			assertEquals(46.77120, cluj.getCoordinates().x(), 0.00001);
			assertEquals(23.62360, cluj.getCoordinates().y(), 0.00001);
		});
		//end::embeddable-Target-fetching-example[]
	}

	//tag::embeddable-Target-example[]
	public interface Coordinates {
		double x();
		double y();
	}

	@Embeddable
	public static class GPS implements Coordinates {

		private double latitude;

		private double longitude;

		private GPS() {
		}

		public GPS(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public double x() {
			return latitude;
		}

		@Override
		public double y() {
			return longitude;
		}
	}

	@Entity(name = "City")
	public static class City {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@Target(GPS.class)
		private Coordinates coordinates;

		//Getters and setters omitted for brevity

	//end::embeddable-Target-example[]

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Coordinates getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(Coordinates coordinates) {
			this.coordinates = coordinates;
		}
	//tag::embeddable-Target-example[]
	}
	//end::embeddable-Target-example[]
}
