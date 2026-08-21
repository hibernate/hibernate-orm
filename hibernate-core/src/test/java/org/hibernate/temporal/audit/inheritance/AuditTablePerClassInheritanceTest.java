/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.inheritance;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests @Audited with TABLE_PER_CLASS inheritance.
 * Each concrete class has its own table and audit table.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditTablePerClassInheritanceTest.Vehicle.class,
		AuditTablePerClassInheritanceTest.Car.class,
		AuditTablePerClassInheritanceTest.SportsCar.class,
		AuditTablePerClassInheritanceTest.Truck.class,
		AuditTablePerClassInheritanceTest.Driver.class,
		AuditTablePerClassInheritanceTest.Team.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.inheritance.AuditTablePerClassInheritanceTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditTablePerClassInheritanceTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}

	}

	// Shared lifecycle: SportsCar(1) + Truck(2), update, delete
	private int revCreate;    // SportsCar(1) + Truck(2)
	private int revUpdate;    // update SportsCar name + seatCount
	private int revTruckUpd;  // update Truck name
	private int revDelete;    // delete SportsCar(1)

	// Deep hierarchy: Car(30) + SportsCar(31), update
	private int revDeepCreate; // Car(30) + SportsCar(31)
	private int revDeepUpdate; // update SportsCar(31)

	// ToOne association: SportsCar(40) + Driver(41), update car
	private int revToOneCreate; // SportsCar(40) + Driver(41)
	private int revToOneUpdate; // update SportsCar(40) name

	// ManyToMany association: SportsCar(50) + Truck(51) + Team(52), update car
	private int revM2mCreate; // SportsCar(50) + Truck(51) + Team(52)
	private int revM2mUpdate; // update SportsCar(50) name

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle (IDs 1-2) ---

		sf.inTransaction( session -> {
			session.persist( new SportsCar( 1L, "Sedan", 5, 200 ) );
			session.persist( new Truck( 2L, "Hauler", 10.5 ) );
		} );
		revCreate = currentTxId;

		sf.inTransaction( session -> {
			var car = session.find( SportsCar.class, 1L );
			car.name = "Sports Car";
			car.seatCount = 2;
		} );
		revUpdate = currentTxId;

		sf.inTransaction( session -> session.find( Truck.class, 2L ).name = "Big Hauler" );
		revTruckUpd = currentTxId;

		sf.inTransaction( session -> session.remove( session.find( SportsCar.class, 1L ) ) );
		revDelete = currentTxId;

		// --- Deep hierarchy (IDs 30-31) ---

		sf.inTransaction( session -> {
			session.persist( new Car( 30L, "Plain Car", 4 ) );
			session.persist( new SportsCar( 31L, "Ferrari", 2, 600 ) );
		} );
		revDeepCreate = currentTxId;

		sf.inTransaction( session -> {
			var sc = session.find( SportsCar.class, 31L );
			sc.name = "Lamborghini";
			sc.horsepower = 700;
		} );
		revDeepUpdate = currentTxId;

		// --- ToOne association (IDs 40-41) ---

		sf.inTransaction( session -> {
			var car = new SportsCar( 40L, "Ferrari", 2, 600 );
			session.persist( car );
			session.persist( new Driver( 41L, "Lewis", car ) );
		} );
		revToOneCreate = currentTxId;

		sf.inTransaction( session -> session.find( SportsCar.class, 40L ).name = "Lamborghini" );
		revToOneUpdate = currentTxId;

		// --- ManyToMany association (IDs 50-52) ---

		sf.inTransaction( session -> {
			var car = new SportsCar( 50L, "Ferrari", 2, 600 );
			var truck = new Truck( 51L, "Hauler", 10.5 );
			session.persist( car );
			session.persist( truck );
			var team = new Team( 52L, "Racing" );
			team.vehicles.add( car );
			team.vehicles.add( truck );
			session.persist( team );
		} );
		revM2mCreate = currentTxId;

		sf.inTransaction( session -> session.find( SportsCar.class, 50L ).name = "Lamborghini" );
		revM2mUpdate = currentTxId;
	}

	@Test
	@Order(1)
	void testWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getChangesets( SportsCar.class, 1L ) ).hasSize( 3 );
			assertThat( auditLog.getChangesets( Truck.class, 2L ) ).hasSize( 2 );
		}
	}

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revCreate: original values
		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var car = s.find( SportsCar.class, 1L );
			assertThat( car ).isNotNull();
			assertThat( car.name ).isEqualTo( "Sedan" );
			assertThat( car.seatCount ).isEqualTo( 5 );

			var truck = s.find( Truck.class, 2L );
			assertThat( truck ).isNotNull();
			assertThat( truck.name ).isEqualTo( "Hauler" );
			assertThat( truck.payload ).isEqualTo( 10.5 );
		}

		// At revUpdate: car updated, truck unchanged. Polymorphic lookups
		try (var s = sf.withOptions().atChangeset( revUpdate ).openSession()) {
			var car = s.find( SportsCar.class, 1L );
			assertThat( car ).isNotNull();
			assertThat( car.name ).isEqualTo( "Sports Car" );
			assertThat( car.seatCount ).isEqualTo( 2 );

			assertThat( s.find( Car.class, 1L ) ).isNotNull().extracting( v -> v.name )
					.isEqualTo( "Sports Car" );
			assertThat( s.find( Vehicle.class, 1L ) ).isNotNull().extracting( v -> v.name )
					.isEqualTo( "Sports Car" );

			assertThat( s.find( Truck.class, 2L ) ).isNotNull()
					.extracting( v -> v.name ).isEqualTo( "Hauler" );
		}

		// At revTruckUpd: truck name updated
		try (var s = sf.withOptions().atChangeset( revTruckUpd ).openSession()) {
			var truck = s.find( Truck.class, 2L );
			assertThat( truck ).isNotNull();
			assertThat( truck.name ).isEqualTo( "Big Hauler" );
			assertThat( truck.payload ).isEqualTo( 10.5 );
		}

		// At revDelete: car deleted
		try (var s = sf.withOptions().atChangeset( revDelete ).openSession()) {
			assertThat( s.find( SportsCar.class, 1L ) ).isNull();
			assertThat( s.find( Truck.class, 2L ) ).isNotNull();
		}
	}

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( SportsCar.class, 1L );
			assertThat( history ).hasSize( 3 );
			assertThat( history.get( 0 ).entity().name ).isEqualTo( "Sedan" );
			assertThat( history.get( 0 ).entity().seatCount ).isEqualTo( 5 );
			assertThat( history.get( 1 ).entity().name ).isEqualTo( "Sports Car" );
			assertThat( history.get( 1 ).entity().seatCount ).isEqualTo( 2 );
			assertThat( history.get( 2 ).entity() ).isNotNull();
		}
	}

	@Test
	@Order(4)
	void testDeepHierarchy(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var auditLog = AuditLogFactory.create( sf )) {
			assertThat( auditLog.getChangesets( Car.class, 30L ) ).hasSize( 1 );
			assertThat( auditLog.getChangesets( SportsCar.class, 31L ) ).hasSize( 2 );
		}

		try (var s = sf.withOptions().atChangeset( revDeepCreate ).openSession()) {
			assertThat( s.find( Car.class, 31L ) ).isNotNull()
					.extracting( v -> v.name ).isEqualTo( "Ferrari" );
		}

		try (var s = sf.withOptions().atChangeset( revDeepUpdate ).openSession()) {
			assertThat( s.find( Car.class, 31L ) ).isNotNull()
					.extracting( v -> v.name ).isEqualTo( "Lamborghini" );
		}

		try (var auditLog = AuditLogFactory.create( sf )) {
			var history = auditLog.getHistory( SportsCar.class, 31L );
			assertThat( history ).hasSize( 2 );
			assertThat( history.get( 0 ).entity().name ).isEqualTo( "Ferrari" );
			assertThat( history.get( 0 ).entity().horsepower ).isEqualTo( 600 );
			assertThat( history.get( 1 ).entity().name ).isEqualTo( "Lamborghini" );
		}
	}

	@Test
	@Order(5)
	void testToOneAssociation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withOptions().atChangeset( revToOneCreate ).openSession()) {
			var driver = s.find( Driver.class, 41L );
			assertThat( driver ).isNotNull();
			assertThat( driver.vehicle ).isNotNull();
			assertThat( driver.vehicle.name ).isEqualTo( "Ferrari" );
		}

		try (var s = sf.withOptions().atChangeset( revToOneUpdate ).openSession()) {
			var driver = s.find( Driver.class, 41L );
			assertThat( driver ).isNotNull();
			assertThat( driver.vehicle.name ).isEqualTo( "Lamborghini" );
		}
	}

	@Test
	@Order(6)
	void testManyToManyAssociation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withOptions().atChangeset( revM2mCreate ).openSession()) {
			var team = s.find( Team.class, 52L );
			assertThat( team ).isNotNull();
			assertThat( team.vehicles ).extracting( v -> v.name )
					.containsExactlyInAnyOrder( "Ferrari", "Hauler" );
		}

		try (var s = sf.withOptions().atChangeset( revM2mUpdate ).openSession()) {
			var team = s.find( Team.class, 52L );
			assertThat( team ).isNotNull();
			assertThat( team.vehicles ).extracting( v -> v.name )
					.containsExactlyInAnyOrder( "Hauler", "Lamborghini" );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Vehicle")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Vehicle {
		@Id
		long id;
		String name;

		Vehicle() {
		}

		Vehicle(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Car")
	static class Car extends Vehicle {
		int seatCount;

		Car() {
		}

		Car(long id, String name, int seatCount) {
			super( id, name );
			this.seatCount = seatCount;
		}
	}

	@Entity(name = "SportsCar")
	static class SportsCar extends Car {
		int horsepower;

		SportsCar() {
		}

		SportsCar(long id, String name, int seatCount, int horsepower) {
			super( id, name, seatCount );
			this.horsepower = horsepower;
		}
	}

	@Entity(name = "Truck")
	static class Truck extends Vehicle {
		double payload;

		Truck() {
		}

		Truck(long id, String name, double payload) {
			super( id, name );
			this.payload = payload;
		}
	}

	@Audited
	@Entity(name = "Driver")
	static class Driver {
		@Id
		long id;
		String driverName;
		@ManyToOne
		Vehicle vehicle;

		Driver() {
		}

		Driver(long id, String driverName, Vehicle vehicle) {
			this.id = id;
			this.driverName = driverName;
			this.vehicle = vehicle;
		}
	}

	@Audited
	@Entity(name = "Team")
	static class Team {
		@Id
		long id;
		String teamName;
		@ManyToMany
		List<Vehicle> vehicles = new ArrayList<>();

		Team() {
		}

		Team(long id, String teamName) {
			this.id = id;
			this.teamName = teamName;
		}
	}
}
