/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				RefreshWithPropertyAccessAndListManipulationInSetterMethodTest.Car.class,
				RefreshWithPropertyAccessAndListManipulationInSetterMethodTest.CarName.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16272")
public class RefreshWithPropertyAccessAndListManipulationInSetterMethodTest {

	@Test
	public void testPersistAndRefresh(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Car car = new Car( "Audi" );
					session.persist( car );

					session.flush();

					session.refresh( car );
				}
		);
	}

	@Entity(name = "Car")
	@Table(name = "car")
	public static class Car {

		@Id
		@GeneratedValue
		@Access(AccessType.PROPERTY)
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		List<CarName> brandNames = new ArrayList<>();

		public Car() {
			for ( Country country : Country.values() ) {
				brandNames.add( new CarName( null, country ) );
			}
		}

		public Car(String name) {
			this();
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
			for ( int i = 0; i < 3; i++ ) {
				CarName carName = brandNames.get( i );
				carName.carId = id;
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<CarName> getBrandNames() {
			return brandNames;
		}

		public void setBrandNames(List<CarName> brandNames) {
			this.brandNames = brandNames;
		}
	}

	@Entity(name = "CarName")
	@Table(name = "carname")
	public static class CarName {

		@Id
		@Column(name = "car_id")
		private Long carId;

		@Id
		Country country;

		public CarName() {
		}

		public CarName(Long carId, Country country) {
			this.carId = carId;
			this.country = country;
		}

		public Long getCarId() {
			return carId;
		}

		public void setCarId(Long carId) {
			this.carId = carId;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	}

	public enum Country {
		FRANCE,
		ENGLAND,
		CZECHIA,
	}
}
