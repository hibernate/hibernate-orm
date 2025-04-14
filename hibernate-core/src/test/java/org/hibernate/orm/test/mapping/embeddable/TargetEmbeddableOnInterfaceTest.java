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
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

public class TargetEmbeddableOnInterfaceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			City.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = new City();
			cluj.setName("Cluj");
			cluj.setCoordinates(new GPS(46.77120, 23.62360));

			entityManager.persist(cluj);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {

			City cluj = entityManager.find(City.class, 1L);

			assertEquals(46.77120, cluj.getCoordinates().x(), 0.00001);
			assertEquals(23.62360, cluj.getCoordinates().y(), 0.00001);
		});
	}

	//tag::embeddable-Target-example2[]
	@TargetEmbeddable(GPS.class)
	public interface Coordinates {
		double x();
		double y();
	}

	@Embeddable
	public static class GPS implements Coordinates {

	// Omitted for brevity

		//end::embeddable-Target-example2[]

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
		//tag::embeddable-Target-example2[]
	}

	@Entity(name = "City")
	public static class City {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private Coordinates coordinates;

		//Getters and setters omitted for brevity

	//end::embeddable-Target-example2[]

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
	//tag::embeddable-Target-example2[]
	}
	//end::embeddable-Target-example2[]
}
