package org.hibernate.temporal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-21221")
@RequiresDialect(value = H2Dialect.class, majorVersion = 2)
@DomainModel(annotatedClasses = TemporalIdentityGeneratedColumnTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = STATEMENT_BATCH_SIZE, value = "5"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class TemporalIdentityGeneratedColumnTest {
	@Test
	void replacementRetainsIdentifierAndReadsGeneratedColumn(SessionFactoryScope scope) {
		final var item = new Item();
		item.quantity = 7;
		final var before = scope.fromTransaction( session -> {
			session.persist( item );
			session.flush();
			assertNotNull( item.id );
			assertEquals( 14, item.doubled );
			return session.getCurrentChangesetIdentifier();
		} );
		final Long id = item.id;
		assertNotNull( before );

		scope.inTransaction( session -> {
			final var managed = session.find( Item.class, id );
			assertNotNull( managed );
			assertEquals( 14, managed.doubled );
			managed.quantity = 13;
			session.flush();

			// The replacement must hydrate the managed instance, not just the next load.
			assertEquals( id, managed.id );
			assertEquals( 26, managed.doubled );
		} );

		scope.inSession( session -> checkVersion( session.find( Item.class, id ), id, 13, 26 ) );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession() ) {
			checkVersion( session.find( Item.class, id ), id, 7, 14 );
		}
	}

	private static void checkVersion(Item item, Long id, int quantity, int doubled) {
		assertNotNull( item );
		assertEquals( id, item.id );
		assertEquals( quantity, item.quantity );
		assertEquals( doubled, item.doubled );
	}

	@Entity(name = "TemporalIdentityGeneratedItem")
	@Temporal
	static class Item {
		@Id
		@GeneratedValue(strategy = IDENTITY)
		Long id;

		@Column(name = "item_quantity", nullable = false)
		int quantity;

		@GeneratedColumn("item_quantity * 2")
		Integer doubled;
	}
}
