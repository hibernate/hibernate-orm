package org.hibernate.temporal.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BytecodeEnhanced
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = TemporalLazyOptimisticLockTest.Item.class)
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class TemporalLazyOptimisticLockTest {
	@Test
	void deleteUnfetched(SessionFactoryScope scope) {
		final var before = persist( scope, 1 );
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertFalse( isPropertyInitialized( item, "body" ) );
			inspector.clear();
			session.remove( item );
			session.flush();
			inspector.assertExecutedCount( 1 );
			inspector.assertIsUpdate( 0 );
		} );
		scope.inSession( session -> assertNull( session.find( Item.class, 1 ) ) );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession() ) {
			assertEquals( "original", session.find( Item.class, 1 ).body );
		}
	}

	@Test
	void rejectStaleDelete(SessionFactoryScope scope) {
		persist( scope, 2 );
		try ( var first = scope.getSessionFactory().openSession() ) {
			final var stale = first.find( Item.class, 2 );
			assertFalse( isPropertyInitialized( stale, "body" ) );
			scope.inTransaction( s -> s.find( Item.class, 2 ).title = "changed" );
			final var tx = first.beginTransaction();
			try {
				first.remove( stale );
				assertThrows( OptimisticLockException.class, first::flush );
			}
			finally {
				tx.rollback();
			}
		}
		scope.inSession( session -> assertEquals( "changed", session.find( Item.class, 2 ).title ) );
	}

	@Test
	void assignUnfetchedAfterEarlierRevision(SessionFactoryScope scope) {
		final var original = persist( scope, 3 );
		final var middle = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 3 );
			assertFalse( isPropertyInitialized( item, "body" ) );
			item.body = null;
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 3 );
			assertFalse( isPropertyInitialized( item, "body" ) );
			item.body = "replacement";
		} );
		scope.inSession( session -> assertEquals( "replacement", session.find( Item.class, 3 ).body ) );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( middle ).openSession() ) {
			assertNull( session.find( Item.class, 3 ).body );
		}
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( original ).openSession() ) {
			assertEquals( "original", session.find( Item.class, 3 ).body );
		}
	}

	@Test
	void rejectStaleLazyValue(SessionFactoryScope scope) {
		persist( scope, 4 );
		try ( var first = scope.getSessionFactory().openSession() ) {
			final var stale = first.find( Item.class, 4 );
			assertEquals( "original", stale.body );
			scope.inTransaction( s -> s.find( Item.class, 4 ).body = "changed" );
			final var tx = first.beginTransaction();
			try {
				stale.title = "stale update";
				assertThrows( OptimisticLockException.class, first::flush );
			}
			finally {
				tx.rollback();
			}
		}
		scope.inSession( session -> {
			final var item = session.find( Item.class, 4 );
			assertEquals( "title", item.title );
			assertEquals( "changed", item.body );
		} );
	}

	private static Object persist(SessionFactoryScope scope, int id) {
		return scope.fromTransaction( session -> {
			final var item = new Item();
			item.id = id;
			item.title = "title";
			item.body = "original";
			session.persist( item );
			return session.getCurrentChangesetIdentifier();
		} );
	}

	@Entity(name = "LockedItem")
	@Temporal @OptimisticLocking(type = OptimisticLockType.ALL) @DynamicUpdate
	static class Item {
		@Id int id;
		String title;
		@Basic(fetch = LAZY) String body;
	}
}
