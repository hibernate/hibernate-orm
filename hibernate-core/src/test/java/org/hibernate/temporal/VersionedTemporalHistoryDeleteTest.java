package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = VersionedTemporalHistoryDeleteTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
})
@JiraKey("HHH-21274")
class VersionedTemporalHistoryDeleteTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1 ) ) );
		awaitTimestampTick();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void removeUnloadedReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var reference = session.getReference( Item.class, 1 );
			assertFalse( Hibernate.isInitialized( reference ) );
			session.remove( reference );
			session.flush();
			assertFalse( Hibernate.isInitialized( reference ) );
		} );
		scope.inTransaction( session -> assertNull( session.find( Item.class, 1 ) ) );
		assertEquals( 1L, countClosedRevisions( scope ) );
	}

	private static long countClosedRevisions(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> session.createNativeQuery(
				"select count(*) from versioned_history_item_history where id = 1 and superseded is not null", Long.class
		).getSingleResult() );
	}

	@Entity(name = "Item")
	@Table(name = "versioned_history_item")
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
}
