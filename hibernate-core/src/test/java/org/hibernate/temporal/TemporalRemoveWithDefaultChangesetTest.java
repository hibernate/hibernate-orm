package org.hibernate.temporal;

import org.hibernate.temporal.TemporalRemoveInSameChangesetTest.VersionedItem;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.temporal.TemporalRemoveInSameChangesetTest.countRevisions;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21276")
@DomainModel(annotatedClasses = VersionedItem.class)
class TemporalRemoveWithDefaultChangesetTest {
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
	})
	void singleTableWithLegacyQueue(SessionFactoryScope scope) {
		persistFlushThenRemove( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
	})
	void historyTableWithGraphQueue(SessionFactoryScope scope) {
		persistFlushThenRemove( scope );
	}

	private static void persistFlushThenRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new VersionedItem( 1 );
			session.persist( item );
			session.flush();
			session.remove( item );
		} );
		assertEquals( 0L, countRevisions( scope, VersionedItem.class ) );
	}
}
