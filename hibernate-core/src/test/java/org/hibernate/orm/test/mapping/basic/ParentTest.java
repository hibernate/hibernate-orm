/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Parent;
import org.hibernate.annotations.TargetEmbeddable;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {ParentTest.City.class} )
public class ParentTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		//tag::mapping-Parent-persist-example[]
		scope.inTransaction( entityManager -> {

			City cluj = new City();
			cluj.setName("Cluj");
			cluj.setCoordinates(new GPS(46.77120, 23.62360));

			entityManager.persist(cluj);
		});
		//end::mapping-Parent-persist-example[]


		//tag::mapping-Parent-fetching-example[]
		scope.inTransaction( entityManager -> {

			City cluj = entityManager.find(City.class, 1L);

			assertSame(cluj, cluj.getCoordinates().getCity());
		});
		//end::mapping-Parent-fetching-example[]
	}

	//tag::mapping-Parent-example[]

	@Embeddable
	public static class GPS {

		private double latitude;

		private double longitude;

		@Parent
		private City city;

		//Getters and setters omitted for brevity

	//end::mapping-Parent-example[]

		private GPS() {
		}

		public GPS(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public City getCity() {
			return city;
		}

		public void setCity(City city) {
			this.city = city;
		}
	//tag::mapping-Parent-example[]
	}
	//end::mapping-Parent-example[]

	//tag::mapping-Parent-example[]

	@Entity(name = "City")
	public static class City {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@TargetEmbeddable(GPS.class)
		private GPS coordinates;

		//Getters and setters omitted for brevity

	//end::mapping-Parent-example[]

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public GPS getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(GPS coordinates) {
			this.coordinates = coordinates;
		}
	//tag::mapping-Parent-example[]
	}
	//end::mapping-Parent-example[]
}
