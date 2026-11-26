package org.hibernate.temporal;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@DomainModel(annotatedClasses = {
		TablePerClassHistoryTest.Vehicle.class,
		TablePerClassHistoryTest.Car.class,
		TablePerClassHistoryTest.ElectricCar.class,
		TablePerClassHistoryTest.Truck.class,
		TablePerClassHistoryTest.Document.class,
		TablePerClassHistoryTest.PublishedDocument.class,
		TablePerClassHistoryTest.Report.class
})
@JiraKey("HHH-21277")
class TablePerClassHistoryTest {
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = CHANGESET_ID_SUPPLIER, value = InstantChangesets.NAME),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
	})
	void graphQueue(SessionFactoryScope scope) {
		testHistory( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = CHANGESET_ID_SUPPLIER, value = InstantChangesets.NAME),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
	})
	void legacyQueue(SessionFactoryScope scope) {
		testHistory( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = CHANGESET_ID_SUPPLIER, value = InstantChangesets.NAME),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
	})
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner does not allow updating primary key columns")
	void rejectStaleHistoryRevisionInGraphQueue(SessionFactoryScope scope) {
		testStaleHistoryRevision( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = CHANGESET_ID_SUPPLIER, value = InstantChangesets.NAME),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
	})
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner does not allow updating primary key columns")
	void rejectStaleHistoryRevisionInLegacyQueue(SessionFactoryScope scope) {
		testStaleHistoryRevision( scope );
	}

	private void testStaleHistoryRevision(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Car( 2, 4 ) ) );
		scope.inTransaction( session -> session.find( Car.class, 2 ).doors = 5 );
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var stale = session.find( Car.class, 2 );
			assertThat( session.createNativeMutationQuery(
					"update tch_car_versions set version = version + 1 where id = 2 and superseded is null"
			).executeUpdate() ).isEqualTo( 1 );
			session.remove( stale );
			session.flush();
		} ) ).isInstanceOf( OptimisticLockException.class );
	}

	private void testHistory(SessionFactoryScope scope) {
		final var insertedAt = scope.fromTransaction( session -> {
			session.persist( new Vehicle( 1 ) );
			session.persist( new Car( 2, 4 ) );
			session.persist( new ElectricCar( 3, 2, 75 ) );
			session.persist( new Truck( 4, 16 ) );
			session.persist( new Report( 5, "draft" ) );
			session.persist( new Truck( 6, 20 ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var updatedAt = scope.fromTransaction( session -> {
			session.find( Car.class, 2 ).doors = 5;
			session.find( ElectricCar.class, 3 ).battery = 80;
			session.find( Report.class, 5 ).title = "published";
			return session.getCurrentChangesetIdentifier();
		} );
		final var deletedAt = scope.fromTransaction( session -> {
			session.remove( session.find( Vehicle.class, 2 ) );
			session.remove( session.find( ElectricCar.class, 3 ) );
			session.remove( session.find( Document.class, 5 ) );
			return session.getCurrentChangesetIdentifier();
		} );

		withHistoryAt( scope, insertedAt, session -> {
			final List<Vehicle> vehicles = session.createSelectionQuery( "from Vehicle order by id", Vehicle.class ).list();
			assertThat( vehicles ).extracting( vehicle -> vehicle.getClass().getSimpleName() )
					.containsExactly( "Vehicle", "Car", "ElectricCar", "Truck", "Truck" );
			assertThat( ((Car) vehicles.get( 1 )).doors ).isEqualTo( 4 );
			assertThat( ((ElectricCar) vehicles.get( 2 )).battery ).isEqualTo( 75 );
			assertThat( ((Truck) vehicles.get( 3 )).capacity ).isEqualTo( 16 );
			assertThat( session.find( ElectricCar.class, 3 ).doors ).isEqualTo( 2 );
			assertThat( ((Report) session.find( Document.class, 5 )).title ).isEqualTo( "draft" );
		} );
		withHistoryAt( scope, updatedAt, session -> {
			assertThat( session.createSelectionQuery(
					"select treat(v as Car).doors from Vehicle v where type(v) = Car", Integer.class
			).getSingleResult() ).isEqualTo( 5 );
			assertThat( ((ElectricCar) session.find( Car.class, 3 )).battery ).isEqualTo( 80 );
			assertThat( session.find( Report.class, 5 ).title ).isEqualTo( "published" );
			assertThat( session.find( PublishedDocument.class, 5 ) ).isExactlyInstanceOf( Report.class );
		} );
		withHistoryAt( scope, deletedAt, session -> {
			assertThat( session.createSelectionQuery( "from Vehicle order by id", Vehicle.class ).list() )
					.extracting( vehicle -> vehicle.id ).containsExactly( 1, 4, 6 );
			assertThat( session.find( Document.class, 5 ) ).isNull();
		} );
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select count(*) from tch_car_versions", Long.class )
					.getSingleResult() ).isEqualTo( 2L );
			assertThat( session.find( Vehicle.class, 1 ) ).isExactlyInstanceOf( Vehicle.class );
			assertThat( session.find( Truck.class, 4 ).capacity ).isEqualTo( 16 );
			assertThat( session.find( ElectricCar.class, 3 ) ).isNull();
		} );
		scope.inTransaction( session -> {
			session.remove( session.find( Truck.class, 6 ) );
			final var reference = session.getReference( Truck.class, 4 );
			assertThat( isInitialized( reference ) ).isFalse();
			session.remove( reference );
			session.flush();
			assertThat( isInitialized( reference ) ).isFalse();
		} );
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery( "from Truck", Truck.class ).list() ).isEmpty();
			assertThat( session.createNativeQuery( "select count(*) from tch_truck_history where superseded is not null", Long.class )
					.getSingleResult() ).isEqualTo( 2L );
		} );
	}

	private void withHistoryAt(SessionFactoryScope scope, Object changeset, Consumer<Session> work) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			session.inTransaction( transaction -> work.accept( session ) );
		}
	}

	@Entity(name = "Vehicle")
	@Table(name = "tch_vehicle")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Temporal
	static class Vehicle {
		@Id
		Integer id;
		@Version
		Integer version;

		Vehicle() {
		}

		Vehicle(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Car")
	@Table(name = "tch_car")
	@Temporal.HistoryTable(name = "tch_car_versions")
	static class Car extends Vehicle {
		@Column(nullable = false)
		int doors;

		Car() {
		}

		Car(int id, int doors) {
			super( id );
			this.doors = doors;
		}
	}

	@Entity(name = "ElectricCar")
	@Table(name = "tch_electric_car")
	static class ElectricCar extends Car {
		int battery;

		ElectricCar() {
		}

		ElectricCar(int id, int doors, int battery) {
			super( id, doors );
			this.battery = battery;
		}
	}

	@Entity(name = "Truck")
	@Table(name = "tch_truck")
	static class Truck extends Vehicle {
		int capacity;

		Truck() {
		}

		Truck(int id, int capacity) {
			super( id );
			this.capacity = capacity;
		}
	}

	@Entity(name = "Document")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Temporal(rowStart = "started_at", rowEnd = "ended_at")
	static abstract class Document {
		@Id
		Integer id;
	}

	@Entity(name = "PublishedDocument")
	static abstract class PublishedDocument extends Document {
	}

	@Entity(name = "Report")
	@Table(name = "tch_report")
	@DynamicInsert
	@DynamicUpdate
	static class Report extends PublishedDocument {
		String title;

		Report() {
		}

		Report(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
