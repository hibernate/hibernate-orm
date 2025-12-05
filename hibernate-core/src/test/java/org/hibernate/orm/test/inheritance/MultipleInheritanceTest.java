/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		MultipleInheritanceTest.CarOptional.class,
		MultipleInheritanceTest.CarPart.class,
		MultipleInheritanceTest.BasicCar.class,
		MultipleInheritanceTest.SuperCar.class,
		MultipleInheritanceTest.Car.class
})
@SessionFactory
public class MultipleInheritanceTest {
	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Car car = new Car();
			CarPart carPart = new CarPart();


			CarPK id = new CarPK();
			id.carId1 = "1";
			carPart.id = id;
			session.persist( carPart );

			car.id = id;
			car.parts = carPart;
			((BasicCar) car).parts = carPart;
			session.persist( car );
			session.flush();
			session.clear();

			Car loadedCar = session.find( Car.class, id );
			assertNotNull( loadedCar.parts );
		} );
	}

	@Embeddable
	public static class CarPK implements Serializable {
		@Column(name = "CAR_ID_1")
		protected String carId1;
	}

	@Entity(name = "BasicCar")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class BasicCar {
		@EmbeddedId
		protected CarPK id;

		@OneToOne
		@JoinColumn(name = "CAR_ID_1", referencedColumnName = "CAR_ID_1", insertable = false, updatable = false)
		CarPart parts;
	}

	@Entity(name = "SuperCar")
	public static class SuperCar extends BasicCar {
		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
//        @JoinColumn(name = "CAR_ID_1", referencedColumnName = "CAR_ID_1")
		private List<CarOptional> optionals;
	}

	@MappedSuperclass
	public static abstract class AbstractCar extends BasicCar {
		@OneToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "CAR_ID_1", referencedColumnName = "CAR_ID_1", insertable = false, updatable = false)
		CarPart parts ;
	}

	@Entity(name = "CarPart")
	public static class CarPart {
		@EmbeddedId
		private CarPK id;

		String name;
	}

	@Entity(name = "Car")
	public static class Car extends AbstractCar {

	}



	@Entity(name = "CarOptional")
	public static class CarOptional {

		@EmbeddedId
		private CarOptionalPK id;

		private String name;

		@Embeddable
		public static class CarOptionalPK implements Serializable {

			@Column(name = "OPTIONAL_ID1")
			private String id1;

		}
	}
}
