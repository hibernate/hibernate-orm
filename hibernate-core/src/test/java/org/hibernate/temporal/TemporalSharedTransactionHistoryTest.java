package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Temporal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// A changeset belongs to a transaction. Sessions which share a transaction
/// share its changeset identifier, and the revisions written when it completes
/// record the net effect of the changes made by all of them.
@JiraKey("HHH-21285")
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalSharedTransactionHistoryTest.SUPPLIER)
})
@DomainModel(annotatedClasses = TemporalSharedTransactionHistoryTest.Thing.class)
@SessionFactory
class TemporalSharedTransactionHistoryTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Entity(name = "SharedThing")
	@Table(name = "shared_thing")
	@Temporal
	static class Thing {
		@Id int id = 1;
		String name;

		Thing() {
		}

		Thing(String name) {
			this.name = name;
		}
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void sessionsShareTheChangesetIdentifier(SessionFactoryScope scope) {
		var changeset = scope.fromTransaction( session -> {
			try ( var child = session.sessionWithOptions().connection().openSession() ) {
				child.persist( new Thing( "a" ) );
				child.flush();
				var childChangeset = ( (SharedSessionContractImplementor) child ).getCurrentChangesetIdentifier();
				assertEquals( childChangeset, session.getCurrentChangesetIdentifier() );
				return childChangeset;
			}
		} );
		assertEquals( List.of( "a" ), revisions( scope ) );
		final long effective = scope.fromSession( session -> session.createNativeQuery(
				"select effective from shared_thing_history", Long.class ).getSingleResult() );
		assertEquals( ( (Number) changeset ).longValue(), effective );
	}

	@Test
	void insertAndUpdateBySessionsSharingTheTransaction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Thing( "a" ) );
			session.flush();
			try ( var child = session.sessionWithOptions().connection().openSession() ) {
				child.find( Thing.class, 1 ).name = "b";
				child.flush();
			}
		} );
		assertEquals( List.of( "b" ), revisions( scope ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	@Test
	void updatesBySessionsSharingTheTransaction(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a" ) ) );
		scope.inTransaction( session -> {
			session.find( Thing.class, 1 ).name = "b";
			session.flush();
			try ( var child = session.sessionWithOptions().connection().openSession() ) {
				child.find( Thing.class, 1 ).name = "c";
				child.flush();
			}
		} );
		assertEquals( List.of( "a", "c" ), revisions( scope ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	@Test
	void removeAndInsertAgainBySessionsSharingTheTransaction(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a" ) ) );
		scope.inTransaction( session -> {
			session.remove( session.find( Thing.class, 1 ) );
			session.flush();
			try ( var child = session.statelessWithOptions().connection().open() ) {
				child.insert( new Thing( "b" ) );
			}
		} );
		assertEquals( List.of( "a", "b" ), revisions( scope ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	private static List<String> revisions(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
				"select name from shared_thing_history order by effective", String.class ).getResultList() );
	}

	private static long openRevisions(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
				"select count(*) from shared_thing_history where superseded is null", Long.class ).getSingleResult() );
	}
}
