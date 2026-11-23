package org.hibernate.temporal.audit;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.JDBC_TIME_ZONE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Timestamp changeset identifiers are instants, and so two changesets
 * an hour apart remain distinct and ordered when the clocks go back
 * in the JDBC time zone.
 */
@JiraKey("HHH-21233")
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditInstantChangesetIdTest.Book.class)
@ServiceRegistry(settings = {
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditInstantChangesetIdTest$Changesets"),
		@Setting(name = JDBC_TIME_ZONE, value = "Europe/Madrid")
})
class AuditInstantChangesetIdTest {
	// in Madrid the clocks go back from 03:00 to 02:00 at 2026-10-25T01:00:00Z,
	// so the local time is 02:30 at both of these instants
	private static final Instant BEFORE_CLOCKS_GO_BACK = Instant.parse( "2026-10-25T00:30:00Z" );
	private static final Instant AFTER_CLOCKS_GO_BACK = Instant.parse( "2026-10-25T01:30:00Z" );

	public static class Changesets implements ChangesetIdentifierSupplier<Instant> {
		private static Instant current;

		@Override
		public Instant generateIdentifier(SharedSessionContract session) {
			return current;
		}
	}

	@Test
	void changesetsEitherSideOfClockChange(SessionFactoryScope scope) {
		Changesets.current = BEFORE_CLOCKS_GO_BACK;
		scope.inTransaction( session -> session.persist( new Book( "first" ) ) );
		Changesets.current = AFTER_CLOCKS_GO_BACK;
		scope.inTransaction( session -> session.find( Book.class, 1 ).title = "second" );

		assertNull( titleAsOf( scope, BEFORE_CLOCKS_GO_BACK.minusSeconds( 1 ) ) );
		assertEquals( "first", titleAsOf( scope, BEFORE_CLOCKS_GO_BACK ) );
		assertEquals( "first", titleAsOf( scope, AFTER_CLOCKS_GO_BACK.minusSeconds( 1 ) ) );
		assertEquals( "second", titleAsOf( scope, AFTER_CLOCKS_GO_BACK ) );
	}

	private static String titleAsOf(SessionFactoryScope scope, Instant instant) {
		try ( var session = scope.getSessionFactory().withOptions().asOf( instant ).openSession() ) {
			final var book = session.find( Book.class, 1 );
			return book == null ? null : book.title;
		}
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id = 1;
		String title;

		Book() {
		}

		Book(String title) {
			this.title = title;
		}
	}
}
