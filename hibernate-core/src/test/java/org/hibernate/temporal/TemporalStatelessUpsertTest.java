package org.hibernate.temporal;

import java.util.List;

import org.hibernate.temporal.TemporalInsertUpdateOrderingTest.Item;
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

@JiraKey("HHH-21276")
@DomainModel(annotatedClasses = Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class TemporalStatelessUpsertTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void insertThenUpsert(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Item( 1, "inserted" ) );
			session.upsert( new Item( 1, "updated" ) );
		} );
		assertRevisions( scope, List.of( "updated" ), "updated" );
	}

	@Test
	void upsertNewRowTwice(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.upsert( new Item( 1, "first" ) );
			session.upsert( new Item( 1, "second" ) );
		} );
		assertRevisions( scope, List.of( "second" ), "second" );
	}

	@Test
	void upsertNewRowThenDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var item = new Item( 1, "new" );
			session.upsert( item );
			session.delete( item );
		} );
		assertRevisions( scope, List.of() );
	}

	@Test
	void upsertExistingRowThenDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.insert( new Item( 1, "original" ) ) );
		scope.inStatelessTransaction( session -> {
			final var item = new Item( 1, "updated" );
			session.upsert( item );
			session.delete( item );
		} );
		assertRevisions( scope, List.of( "original" ) );
	}

	@Test
	void repeatedUpsertPreservesClosedRevision(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.insert( new Item( 1, "original" ) ) );
		scope.inStatelessTransaction( session -> {
			session.upsert( new Item( 1, "first" ) );
			session.upsert( new Item( 1, "second" ) );
		} );
		assertRevisions( scope, List.of( "original", "second" ), "second" );
	}

	@Test
	void upsertAfterDeletingNewRevision(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var item = new Item( 1, "inserted" );
			session.insert( item );
			session.delete( item );
			session.upsert( new Item( 1, "reinserted" ) );
		} );
		assertRevisions( scope, List.of( "reinserted" ), "reinserted" );
	}

	@Test
	void batchedUpsertsOfNewRow(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session ->
				session.upsertMultiple( List.of( new Item( 1, "first" ), new Item( 1, "second" ) ) ) );
		assertRevisions( scope, List.of( "second" ), "second" );
	}

	private static void assertRevisions(SessionFactoryScope scope, List<String> revisions, String... current) {
		scope.inSession( session -> {
			assertEquals( revisions, session.createNativeQuery(
					"select name from Item order by effective", String.class ).list() );
			assertEquals( List.of( current ), session.createSelectionQuery(
					"select i.name from Item i", String.class ).list() );
		} );
	}
}
