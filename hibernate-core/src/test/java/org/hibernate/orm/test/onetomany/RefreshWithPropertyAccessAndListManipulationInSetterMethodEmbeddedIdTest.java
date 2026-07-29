package org.hibernate.orm.test.onetomany;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DomainModel(
		annotatedClasses = {
				RefreshWithPropertyAccessAndListManipulationInSetterMethodEmbeddedIdTest.Car.class,
				RefreshWithPropertyAccessAndListManipulationInSetterMethodEmbeddedIdTest.CarName.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16272")
public class RefreshWithPropertyAccessAndListManipulationInSetterMethodEmbeddedIdTest {

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

		@OneToMany(mappedBy = "car", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		List<CarName> brandNames = new ArrayList<>();

		public Car() {
			for ( Country country : Country.values() ) {
				brandNames.add( new CarName( this, country ) );
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
		@EmbeddedId
		Id id;
		@MapsId("carId")
		@ManyToOne(fetch = FetchType.LAZY)
		Car car;
		@Embeddable
		record Id(@Column(name = "car_id") Long carId, Country country) {}

		public CarName() {
		}

		public CarName(Car car, Country country) {
			this.id = new Id(car.getId(), country);
			this.car = car;
		}
	}

	public enum Country {
		FRANCE,
		ENGLAND,
		CZECHIA,
	}
}
