package org.hibernate.temporal;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class,
		comment = "The second flush increments a version column in the temporal primary key")
class TemporalGeneratedStateTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void singleTable(SessionFactoryScope scope) {
		checkGeneratedValues( scope );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void historyTable(SessionFactoryScope scope) {
		checkGeneratedValues( scope );
	}

	private void checkGeneratedValues(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			final var item = new Item();
			item.name = "first";
			session.persist( item );
			return session.getCurrentChangesetIdentifier();
		} );
		final var after = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			item.name = "second";
			session.flush();
			assertState( item, "IU", 1 );
			session.refresh( item );
			assertState( item, "IU", 1 );
			item.name = "third";
			session.flush();
			assertState( item, "IUU", 2 );
			return session.getCurrentChangesetIdentifier();
		} );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession() ) {
			assertState( session.find( Item.class, 1 ), "I", 0 );
		}
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( after ).openSession() ) {
			assertState( session.find( Item.class, 1 ), "IUU", 2 );
		}
		final var last = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertState( item, "IUU", 2 );
			item.name = "fourth";
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( last ).openSession() ) {
			assertState( session.find( Item.class, 1 ), "IUUU", 3 );
		}
		scope.inSession( session -> assertState( session.find( Item.class, 1 ), "IUUU", 3 ) );
	}

	private static void assertState(Item item, String events, int version) {
		assertEquals( "I", item.created );
		assertEquals( events, item.events );
		assertEquals( version == 0 ? null : "U".repeat( version ), item.modified );
		assertEquals( version, item.version );
	}

	@Entity(name = "GeneratedItem")
	@Temporal
	static class Item {
		@Id int id = 1;
		@Version int version;
		String name;
		@RecordEvents String created;
		@RecordEvents({ INSERT, UPDATE }) String events;
		@RecordEvents(UPDATE) String modified;
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@ValueGenerationType(generatedBy = EventRecorder.class)
	@interface RecordEvents {
		EventType[] value() default INSERT;
	}

	public static class EventRecorder implements BeforeExecutionGenerator {
		private final EnumSet<EventType> events;

		public EventRecorder(RecordEvents annotation) {
			events = EventTypeSets.fromArray( annotation.value() );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return events;
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType event) {
			return (currentValue == null ? "" : currentValue.toString()) + (event == INSERT ? "I" : "U");
		}
	}
}
