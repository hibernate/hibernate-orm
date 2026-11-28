package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// An upsert increments the version of an existing row only when versioned
/// state changes. Where the database lacks `is distinct from`, the compared
/// parameter is rendered twice, and where the dialect executes an upsert as an
/// update followed by an insert, that statement binds it at each position.
@JiraKey("HHH-21288")
@DomainModel(annotatedClasses = UpsertConditionalVersionIncrementTest.Item.class)
@SessionFactory
class UpsertConditionalVersionIncrementTest {
	@Entity(name = "VersionedItem")
	static class Item {
		@Id int id;
		@Version Integer version;
		String name;
		@ExcludedFromVersioning int counter;
	}

	@Test
	void upsertIncrementsVersionOnlyWhenVersionedStateChanges(SessionFactoryScope scope) {
		var item = new Item();
		item.id = 1;
		item.name = "a";
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		assertEquals( 0, item.version );

		item.counter++;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		assertEquals( 0, item.version );

		item.name = "b";
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		assertEquals( 1, item.version );
		scope.inSession( session -> {
			var stored = session.find( Item.class, 1 );
			assertEquals( "b", stored.name );
			assertEquals( 1, stored.counter );
			assertEquals( 1, stored.version );
		} );
	}

	@Test
	void upsertTreatsNullAsChangeAndChecksVersion(SessionFactoryScope scope) {
		var item = new Item();
		item.id = 2;
		item.name = "initial";
		scope.inStatelessTransaction( session -> session.insert( item ) );

		item.name = null;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		assertEquals( 1, item.version );

		item.version = 0;
		item.name = "stale";
		assertThrows( OptimisticLockException.class,
				() -> scope.inStatelessTransaction( session -> session.upsert( item ) ) );
		scope.inSession( session -> {
			var stored = session.find( Item.class, 2 );
			assertEquals( 1, stored.version );
			assertNull( stored.name );
		} );
	}
}
