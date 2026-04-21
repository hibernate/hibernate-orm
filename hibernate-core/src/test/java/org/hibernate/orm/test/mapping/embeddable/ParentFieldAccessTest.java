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
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

@Jpa(annotatedClasses = ParentFieldAccessTest.City.class)
public class ParentFieldAccessTest {

	@Test
	public void testFieldAccessWithoutGetterOrSetter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final var city = new City();
			city.name = "Cluj";
			city.coordinates = new GPS( 46.77120, 23.62360 );

			entityManager.persist( city );
		} );

		scope.inTransaction( entityManager -> {
			final var city = entityManager.find( City.class, 1L );
			assertSame( city, city.coordinates.city );
		} );
	}

	@Embeddable
	public static class GPS {
		private double latitude;
		private double longitude;

		@Parent
		private City city;

		private GPS() {
		}

		private GPS(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	@Entity(name = "CityFieldParent")
	public static class City {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private GPS coordinates;
	}
}
