package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = {
		VersionedTemporalDeleteTest.Item.class,
		VersionedTemporalDeleteTest.Vehicle.class,
		VersionedTemporalDeleteTest.Car.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
})
@JiraKey("HHH-21274")
class VersionedTemporalDeleteTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1 ) );
			session.persist( new Car( 1 ) );
		} );
		awaitTimestampTick();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void removeUnloadedReference(SessionFactoryScope scope) {
		removeUnloadedReference( scope, Item.class );
		assertEquals( 1L, countClosedRevisions( scope, "temporal_versioned_item" ) );
	}

	@Test
	void removeUnloadedTablePerClassLeaf(SessionFactoryScope scope) {
		removeUnloadedReference( scope, Car.class );
		assertEquals( 1L, countClosedRevisions( scope, "temporal_delete_car" ) );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
	})
	@JiraKey("HHH-21264")
	void removeUnloadedReferenceInLegacyQueue(SessionFactoryScope scope) {
		removeUnloadedReference( scope, Item.class );
		assertEquals( 1L, countClosedRevisions( scope, "temporal_versioned_item" ) );
		removeUnloadedReference( scope, Car.class );
		assertEquals( 1L, countClosedRevisions( scope, "temporal_delete_car" ) );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner does not allow updating primary key columns")
	void rejectStaleLoadedRevision(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				assertEquals( 1, session.createNativeMutationQuery(
						"update temporal_versioned_item set version = version + 1 where id = 1 and superseded is null"
				).executeUpdate() );
				session.remove( item );
				assertThrows( OptimisticLockException.class, session::flush );
			}
			finally {
				transaction.rollback();
			}
		} );
		assertEquals( 0L, countClosedRevisions( scope, "temporal_versioned_item" ) );
	}

	private static void removeUnloadedReference(SessionFactoryScope scope, Class<?> entityClass) {
		scope.inTransaction( session -> {
			final var reference = session.getReference( entityClass, 1 );
			assertFalse( Hibernate.isInitialized( reference ) );
			session.remove( reference );
			session.flush();
			assertFalse( Hibernate.isInitialized( reference ) );
		} );
	}

	private static long countClosedRevisions(SessionFactoryScope scope, String table) {
		return scope.fromTransaction( session -> session.createNativeQuery(
				"select count(*) from " + table + " where id = 1 and superseded is not null", Long.class
		).getSingleResult() );
	}

	@Entity(name = "Item")
	@Table(name = "temporal_versioned_item")
	@Temporal
	static class Item {
		@Id
		Integer id;
		@Version
		Integer version;

		Item() {
		}

		Item(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Vehicle")
	@Inheritance(strategy = TABLE_PER_CLASS)
	@Temporal
	static abstract class Vehicle {
		@Id
		Integer id;
		@Version
		Integer version;
	}

	@Entity(name = "Car")
	@Table(name = "temporal_delete_car")
	static class Car extends Vehicle {
		Car() {
		}

		Car(Integer id) {
			this.id = id;
		}
	}
}
