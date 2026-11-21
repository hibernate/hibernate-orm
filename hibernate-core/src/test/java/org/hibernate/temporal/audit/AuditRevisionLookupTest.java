package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.NaturalId;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.KeyType.NATURAL;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lookups by key in an all-changesets session read current state, a refreshed snapshot
 * keeps its revision, and the joined tables of a snapshot are read at its revision.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditRevisionLookupTest.Image.class,
		AuditRevisionLookupTest.Event.class,
		AuditRevisionLookupTest.Vehicle.class,
		AuditRevisionLookupTest.Car.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditRevisionLookupTest$ChangesetIds"))
class AuditRevisionLookupTest {

	private static Object historyCreatedFor;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		// changeset 1
		scope.inTransaction( session -> {
			final var image = new Image();
			image.id = 1;
			image.code = "first";
			image.title = "Original";
			session.persist( image );
			final var other = new Image();
			other.id = 2;
			other.code = "second";
			other.title = "Other original";
			session.persist( other );
			final var car = new Car();
			car.id = 1;
			car.name = "Original";
			car.horsepower = 100;
			session.persist( car );
			final var event = new Event();
			event.id = 1;
			event.label = "First";
			event.vehicle = car;
			session.persist( event );
		} );
		// changeset 2
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "Revised";
			final var car = session.find( Car.class, 1 );
			car.name = "Revised";
			car.horsepower = 200;
			session.find( Event.class, 1 ).label = "Second";
		} );
		// changeset 3
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "Current";
			final var car = session.find( Car.class, 1 );
			car.name = "Current";
			car.horsepower = 300;
		} );
	}

	@Test
	void findMultipleReadsCurrentState(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			assertEquals( List.of( "Current", "Other original" ),
					session.findMultiple( Image.class, List.of( 1, 2 ) ).stream().map( image -> image.title ).toList() );
		}
	}

	@Test
	void naturalIdReadsCurrentState(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			assertEquals( "Current", session.find( Image.class, "first", NATURAL ).title );
		}
		try (var session = allChangesets( scope )) {
			assertEquals( List.of( "Current", "Other original" ),
					session.findMultiple( Image.class, List.of( "first", "second" ), NATURAL )
							.stream().map( image -> image.title ).toList() );
		}
	}

	@Test
	void refreshedSnapshotKeepsItsRevision(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var revisions = session.createSelectionQuery(
					"from Image i where i.id = 1 order by changesetId(i)", Image.class ).getResultList();
			assertEquals( 3, revisions.size() );
			final var first = revisions.get( 0 );
			session.refresh( first );
			assertEquals( "Original", first.title );
		}
	}

	@Test
	void joinedSubclassOfSnapshotAtItsRevision(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var events = session.createSelectionQuery(
					"from Event e join fetch e.vehicle where e.id = 1 order by changesetId(e)", Event.class )
					.getResultList();
			assertEquals( List.of( "Original/100", "Revised/200" ), events.stream().map( event -> {
				final var car = (Car) Hibernate.unproxy( event.vehicle );
				return car.name + "/" + car.horsepower;
			} ).toList() );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var event = session.createSelectionQuery(
					"from Event e join fetch e.vehicle where e.id = 1", Event.class ).getSingleResult();
			final var car = (Car) Hibernate.unproxy( event.vehicle );
			assertEquals( "Original/100", car.name + "/" + car.horsepower );
		}
	}

	private static Session allChangesets(SessionFactoryScope scope) {
		return scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession();
	}

	@Entity(name = "Image")
	@Audited
	static class Image {
		@Id int id;
		@NaturalId String code;
		String title;
	}

	@Entity(name = "Event")
	@Audited
	static class Event {
		@Id int id;
		String label;
		@ManyToOne(fetch = LAZY) Vehicle vehicle;
	}

	@Entity(name = "Vehicle")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	static class Vehicle {
		@Id int id;
		String name;
	}

	@Entity(name = "Car")
	@Audited
	static class Car extends Vehicle {
		int horsepower;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
