package org.hibernate.temporal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.Temporal;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A one-to-many association writes its join column and order column to the
/// table of the concrete class of each associated entity, and each of those
/// tables has a history table of its own.
@JiraKey("HHH-21285")
@DomainModel(annotatedClasses = {
		TemporalOneToManyTablePerClassHistoryTest.Garage.class,
		TemporalOneToManyTablePerClassHistoryTest.Vehicle.class,
		TemporalOneToManyTablePerClassHistoryTest.Car.class,
		TemporalOneToManyTablePerClassHistoryTest.Truck.class
})
class TemporalOneToManyTablePerClassHistoryTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Entity(name = "OtmGarage")
	@Table(name = "otm_garage")
	@Temporal
	static class Garage {
		@Id int id = 1;
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "garage_id")
		@OrderColumn(name = "pos")
		List<Vehicle> vehicles = new ArrayList<>();
	}

	@Entity(name = "OtmVehicle")
	@Temporal
	@Inheritance(strategy = TABLE_PER_CLASS)
	abstract static class Vehicle {
		@Id int id;
	}

	@Entity(name = "OtmCar")
	@Table(name = "otm_car")
	static class Car extends Vehicle {
		Car() {
		}

		Car(int id) {
			this.id = id;
		}
	}

	@Entity(name = "OtmTruck")
	@Table(name = "otm_truck")
	static class Truck extends Vehicle {
		Truck() {
		}

		Truck(int id) {
			this.id = id;
		}
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = FLUSH_QUEUE_TYPE, value = "graph"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void graphQueue(SessionFactoryScope scope) {
		testHistory( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void legacyQueue(SessionFactoryScope scope) {
		testHistory( scope );
	}

	private void testHistory(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			var garage = new Garage();
			garage.vehicles.add( new Car( 10 ) );
			garage.vehicles.add( new Truck( 11 ) );
			garage.vehicles.add( new Car( 12 ) );
			session.persist( garage );
		} );
		var reordered = change( scope, session -> {
			var vehicles = session.find( Garage.class, 1 ).vehicles;
			vehicles.add( vehicles.remove( 0 ) );
		} );
		var removed = change( scope, session -> session.find( Garage.class, 1 ).vehicles.remove( 0 ) );
		var deleted = change( scope, session -> session.remove( session.find( Garage.class, 1 ) ) );

		assertEquals( List.of( 10, 11, 12 ), vehiclesAt( scope, created ) );
		assertEquals( List.of( 11, 12, 10 ), vehiclesAt( scope, reordered ) );
		assertEquals( List.of( 12, 10 ), vehiclesAt( scope, removed ) );
		assertNull( vehiclesAt( scope, deleted ) );
		// 10 and 12 were moved twice and detached, and 11 was moved once and detached
		assertEquals( 4L, count( scope, "select count(*) from otm_car_history where id = 10" ) );
		assertEquals( 3L, count( scope, "select count(*) from otm_truck_history where id = 11" ) );
		assertEquals( 4L, count( scope, "select count(*) from otm_car_history where id = 12" ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_car_history"
				+ " where superseded is null and garage_id is null" ) );
		assertEquals( 1L, count( scope, "select count(*) from otm_truck_history"
				+ " where superseded is null and garage_id is null" ) );
	}

	private static Object change(SessionFactoryScope scope, Consumer<SessionImplementor> work) {
		return scope.fromTransaction( session -> {
			work.accept( session );
			return session.getCurrentChangesetIdentifier();
		} );
	}

	private static List<Integer> vehiclesAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			var garage = session.find( Garage.class, 1 );
			return garage == null ? null : garage.vehicles.stream().map( vehicle -> vehicle.id ).toList();
		}
	}

	private static long count(SessionFactoryScope scope, String sql) {
		return scope.fromSession( session -> session.createNativeQuery( sql, Long.class ).getSingleResult() );
	}
}
