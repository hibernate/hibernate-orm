package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;

@DomainModel(annotatedClasses = {
		TablePerClassVersionedTemporalMutationTest.Vehicle.class,
		TablePerClassVersionedTemporalMutationTest.Car.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
@JiraKey("HHH-21271")
class TablePerClassVersionedTemporalMutationTest {
	@Test
	void closeRootRevisionWithVersionCheck(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Vehicle( 1 ) ) );
		awaitTimestampTick();
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.find( Vehicle.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).anyMatch( sql ->
				sql.matches( "(?is)update temporal_versioned_vehicle\\b.*\\bwhere\\b.*\\bversion\\s*=\\s*\\?.*" ) );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select count(*) from temporal_versioned_vehicle where id = 1 and superseded is not null", Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
	})
	@JiraKey("HHH-21275")
	void persistRootAndLeafWithHistory(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Vehicle( 1 ) );
			final var car = new Car();
			car.id = 2;
			session.persist( car );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Vehicle.class, 1 ) ).isExactlyInstanceOf( Vehicle.class );
			assertThat( session.find( Car.class, 2 ) ).isNotNull();
			assertThat( session.createNativeQuery(
					"select id from temporal_versioned_vehicle_history where effective is not null and superseded is null",
					Integer.class
			).getResultList() ).containsExactly( 1 );
			assertThat( session.createNativeQuery(
					"select id from temporal_versioned_car_history where effective is not null and superseded is null",
					Integer.class
			).getResultList() ).containsExactly( 2 );
		} );
	}

	@Entity(name = "Vehicle")
	@Table(name = "temporal_versioned_vehicle")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Temporal
	public static class Vehicle {
		@Id
		Integer id;
		@Version
		Integer version;

		Vehicle() {
		}

		Vehicle(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Car")
	@Table(name = "temporal_versioned_car")
	public static class Car extends Vehicle {
	}
}
