/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud.onetomany.bidirectional;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.boot.MetadataSources;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

/**
 * @author Chris Cranford
 */
public class QuickOneToManyTest extends SessionFactoryBasedFunctionalTest {
	@Entity(name = "Owner")
	public static class Owner {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany
		@JoinColumn(name = "car_id", referencedColumnName = "id")
		private List<Car> cars = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Car> getCars() {
			return cars;
		}

		public void setCars(List<Car> cars) {
			this.cars = cars;
		}
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Owner.class );
		metadataSources.addAnnotatedClass( Car.class );
	}

	private Integer carId;

	@Test
	public void testSave() {
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car = new Car();
					session.save( car );
					this.carId =  car.getId();
				}
		);
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car = session.find( Car.class, this.carId );

					final Owner owner = new Owner();
					owner.getCars().add( car );
					session.save( owner );
				}
		);
	}
}
