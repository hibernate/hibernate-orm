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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * @author Jan Schatteman
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = TargetEmbeddableOnEmbeddedTest.City.class )
@SessionFactory
public class TargetEmbeddableOnEmbeddedTest {
	@Test
	public void testLifecycle(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			City cluj = new City();
			cluj.setName( "Cluj" );
			cluj.setCoordinates( new GPS(46.77120, 23.62360 ) );

			session.persist( cluj );
		} );

		factoryScope.inTransaction( (session) -> {
//tag::embeddable-Target-fetching-example[]
			City city = session.find(City.class, 1L);
			assert city.getCoordinates() instanceof GPS;
//end::embeddable-Target-fetching-example[]
			assertThat( city.getCoordinates().x() ).isCloseTo( 46.77120, offset( 0.00001 ) );
			assertThat( city.getCoordinates().y() ).isCloseTo( 23.62360, offset( 0.00001 ) );
		} );
	}

//tag::embeddable-Target-example[]
	public interface Coordinates {
		double x();
		double y();
	}

	@Embeddable
	public static class GPS implements Coordinates {
		// Omitted for brevity

//end::embeddable-Target-example[]

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

//tag::embeddable-Target-example[]
	}

	@Entity(name = "City")
	public static class City {
		// Omitted for brevity

//end::embeddable-Target-example[]
		@Id
		@GeneratedValue
		private Long id;

		private String name;

//tag::embeddable-Target-example[]
		@Embedded
		@TargetEmbeddable(GPS.class)
		private Coordinates coordinates;
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
