package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {
		TemporalDetachRefreshTest.TemporalThing.class,
		TemporalDetachRefreshTest.AuditedThing.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.TemporalDetachRefreshTest$ChangesetIds")
})
class TemporalDetachRefreshTest {
	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}

	@Test
	void temporal(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TemporalThing( 1, "v1" ) ) );
		final var changeset = scope.fromTransaction( session -> {
			session.find( TemporalThing.class, 1 ).name = "v2";
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( TemporalThing.class, 1 ).name = "v3" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var thing = session.find( TemporalThing.class, 1 );
			session.detach( thing );
			assertFalse( session.contains( thing ) );
			final var reloaded = session.find( TemporalThing.class, 1 );
			assertNotSame( thing, reloaded );
			assertTrue( session.contains( reloaded ) );
			assertEquals( "v2", reloaded.name );
			session.refresh( reloaded );
			assertEquals( "v2", reloaded.name );
			assertSame( reloaded, session.find( TemporalThing.class, 1 ) );
		}
	}

	@Test
	void audited(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new AuditedThing( 1, "v1" ) ) );
		final var changeset = scope.fromTransaction( session -> {
			session.find( AuditedThing.class, 1 ).name = "v2";
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( AuditedThing.class, 1 ).name = "v3" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var thing = session.find( AuditedThing.class, 1 );
			session.detach( thing );
			assertFalse( session.contains( thing ) );
			final var reloaded = session.find( AuditedThing.class, 1 );
			assertNotSame( thing, reloaded );
			assertTrue( session.contains( reloaded ) );
			assertEquals( "v2", reloaded.name );
			session.refresh( reloaded );
			assertEquals( "v2", reloaded.name );
			assertSame( reloaded, session.find( AuditedThing.class, 1 ) );
		}
	}

	@Entity(name = "TemporalThing")
	@Temporal
	static class TemporalThing {
		@Id int id;
		String name;

		TemporalThing() {
		}

		TemporalThing(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "AuditedThing")
	@Audited
	static class AuditedThing {
		@Id int id;
		String name;

		AuditedThing() {
		}

		AuditedThing(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
