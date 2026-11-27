package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// An SQL expression which generates a value on update may refer to the existing row.
@JiraKey("HHH-21288")
@DomainModel(annotatedClasses = UpsertGeneratedUpdateExpressionTest.Tally.class)
@SessionFactory
class UpsertGeneratedUpdateExpressionTest {
	@Entity(name = "Tally")
	static class Tally {
		@Id
		long id;
		String name;
		@Generated(event = EventType.UPDATE, sql = "counter + 1")
		Integer counter;
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void upsertExistingRow(SessionFactoryScope scope) {
		final var tally = new Tally();
		tally.id = 1;
		tally.name = "a";
		tally.counter = 0;
		scope.inStatelessTransaction( session -> session.insert( tally ) );

		tally.name = "b";
		scope.inStatelessTransaction( session -> session.upsert( tally ) );
		scope.inSession( session -> {
			final var stored = session.find( Tally.class, 1L );
			assertEquals( "b", stored.name );
			assertEquals( 1, stored.counter );
		} );

		tally.name = "c";
		scope.inStatelessTransaction( session -> session.upsert( tally ) );
		scope.inSession( session -> {
			final var stored = session.find( Tally.class, 1L );
			assertEquals( "c", stored.name );
			assertEquals( 2, stored.counter );
		} );
	}

	@Test
	void upsertMissingRow(SessionFactoryScope scope) {
		final var tally = new Tally();
		tally.id = 2;
		tally.name = "a";
		scope.inStatelessTransaction( session -> session.upsert( tally ) );
		scope.inSession( session -> {
			final var stored = session.find( Tally.class, 2L );
			assertEquals( "a", stored.name );
			// the insert leaves the column to the database
			assertNull( stored.counter );
		} );
	}
}
