package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// A dialect which emulates `is distinct from` renders the compared parameter twice.
@JiraKey("HHH-21288")
@DomainModel(annotatedClasses = UpsertVersionEmulatedDistinctFromTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.stateless.UpsertVersionEmulatedDistinctFromTest$EmulatingDialect"))
class UpsertVersionEmulatedDistinctFromTest {
	public static class EmulatingDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
		}

		@Override
		public PredicateSupport getPredicateSupport() {
			return PredicateSupport.STANDARD;
		}
	}

	@Entity(name = "VersionedItem")
	static class Item {
		@Id int id;
		@Version Integer version;
		String name;
		@ExcludedFromVersioning int counter;
	}

	@Test
	void fallbackUpsertBindsRepeatedParameter(SessionFactoryScope scope) {
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
	void fallbackUpsertBindsOldAndNewVersions(SessionFactoryScope scope) {
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
