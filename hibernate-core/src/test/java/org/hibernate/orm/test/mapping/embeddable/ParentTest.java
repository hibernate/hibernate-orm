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

import org.hibernate.annotations.Parent;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class ParentTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			City.class,
		};
	}

	@Test
	public void testLifecycle() {
		//tag::embeddable-Parent-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = new City();
			cluj.setName("Cluj");
			cluj.setCoordinates(new GPS(46.77120, 23.62360));

			entityManager.persist(cluj);
		});
		//end::embeddable-Parent-persist-example[]


		//tag::embeddable-Parent-fetching-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = entityManager.find(City.class, 1L);

			assertSame(cluj, cluj.getCoordinates().getCity());
		});
		//end::embeddable-Parent-fetching-example[]
	}

	//tag::embeddable-Parent-example[]

	@Embeddable
	public static class GPS {

		private double latitude;

		private double longitude;

		@Parent
		private City city;

		//Getters and setters omitted for brevity

	//end::embeddable-Parent-example[]

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
	//tag::embeddable-Parent-example[]
	}
	//end::embeddable-Parent-example[]

	//tag::embeddable-Parent-example[]

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

	//end::embeddable-Parent-example[]

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
	//tag::embeddable-Parent-example[]
	}
	//end::embeddable-Parent-example[]
}
