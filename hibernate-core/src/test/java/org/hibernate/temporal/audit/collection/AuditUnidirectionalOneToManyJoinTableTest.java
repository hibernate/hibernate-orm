/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests unidirectional @OneToMany with @JoinTable auditing.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditUnidirectionalOneToManyJoinTableTest.Team.class,
		AuditUnidirectionalOneToManyJoinTableTest.Player.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditUnidirectionalOneToManyJoinTableTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditUnidirectionalOneToManyJoinTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// Shared lifecycle: Team(1) + players, add/remove/delete
	private int revCreate;  // Team(1) + Player(1, "Alice")
	private int revAdd;     // add Player(2, "Bob")
	private int revRemove;  // remove Player(1)
	private int revDelete;  // delete Team(1)

	// Recreate scenario (IDs 10-19)
	private int revRecCreate;  // Team(10) + Player(10)+Player(11)
	private int revRecReplace; // clear, re-add Player(11) + new Player(12)

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle ---

		// Rev 1: team + one player
		sf.inTransaction( session -> {
			var player = new Player( 1L, "Alice" );
			session.persist( player );
			var team = new Team( 1L, "Red Team" );
			team.players.add( player );
			session.persist( team );
		} );
		revCreate = currentTxId;

		// Rev 2: add second player
		sf.inTransaction( session -> {
			var player = new Player( 2L, "Bob" );
			session.persist( player );
			var team = session.find( Team.class, 1L );
			team.players.add( player );
		} );
		revAdd = currentTxId;

		// Rev 3: remove first player from team
		sf.inTransaction( session -> {
			var team = session.find( Team.class, 1L );
			team.players.removeIf( p -> p.id == 1L );
		} );
		revRemove = currentTxId;

		// Rev 4: delete team (bulk removal of remaining players from collection)
		sf.inTransaction( session -> {
			var team = session.find( Team.class, 1L );
			session.remove( team );
		} );
		revDelete = currentTxId;

		// --- Recreate scenario ---

		// Rev 5: team with Alice + Bob
		sf.inTransaction( session -> {
			var p1 = new Player( 10L, "Rec Alice" );
			var p2 = new Player( 11L, "Rec Bob" );
			session.persist( p1 );
			session.persist( p2 );
			var team = new Team( 10L, "Rec Team" );
			team.players.add( p1 );
			team.players.add( p2 );
			session.persist( team );
		} );
		revRecCreate = currentTxId;

		// Rev 6: recreate: clear and re-add Bob + new Charlie
		sf.inTransaction( session -> {
			var p3 = new Player( 12L, "Rec Charlie" );
			session.persist( p3 );
			var team = session.find( Team.class, 10L );
			team.players.clear();
			team.players.add( session.find( Player.class, 11L ) );
			team.players.add( p3 );
		} );
		revRecReplace = currentTxId;
	}

	// --- Write side verification ---

	@Test
	@Order(1)
	void testWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Team: ADD + 2 collection changes + DEL = 4 revisions
			assertEquals( 4, auditLog.getRevisions( Team.class, 1L ).size(),
					"Team should have 4 revisions (ADD + 2 collection changes + DEL)" );
			assertEquals( 1, auditLog.getRevisions( Player.class, 1L ).size() );
			assertEquals( 1, auditLog.getRevisions( Player.class, 2L ).size() );
		}
	}

	// --- Point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revCreate: team should have 1 player (Alice)
		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var team = s.find( Team.class, 1L );
			assertNotNull( team );
			assertEquals( 1, team.players.size(), "At revCreate, team should have 1 player" );
			assertEquals( "Alice", team.players.get( 0 ).name );
		}

		// At revAdd: team should have 2 players
		try (var s = sf.withOptions().atChangeset( revAdd ).openSession()) {
			var team = s.find( Team.class, 1L );
			assertNotNull( team );
			assertEquals( 2, team.players.size(), "At revAdd, team should have 2 players" );
		}

		// At revRemove: 1 player (Bob only)
		try (var s = sf.withOptions().atChangeset( revRemove ).openSession()) {
			var team = s.find( Team.class, 1L );
			assertNotNull( team );
			assertEquals( 1, team.players.size(), "At revRemove, team should have 1 player" );
			assertEquals( "Bob", team.players.get( 0 ).name );
		}
	}

	// --- getHistory ---

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Team.class, 1L );
			assertEquals( 4, history.size(), "Team has ADD + 2 collection changes + DEL" );
			assertEquals( "Red Team", history.get( 0 ).entity().name );
		}
	}

	// --- Recreate scenario ---

	@Test
	@Order(4)
	void testPointInTimeReadAfterRecreate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Team: ADD + recreate = 2 revisions (not more)
		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getRevisions( Team.class, 10L ).size(),
					"Team should have exactly 2 revisions (ADD + recreate)" );
		}

		// At revRecCreate: 2 players
		try (var s = sf.withOptions().atChangeset( revRecCreate ).openSession()) {
			var team = s.find( Team.class, 10L );
			assertNotNull( team );
			assertEquals( 2, team.players.size(), "At revRecCreate, team should have 2 players" );
		}

		// At revRecReplace: 2 players (Bob + Charlie, Alice dropped)
		try (var s = sf.withOptions().atChangeset( revRecReplace ).openSession()) {
			var team = s.find( Team.class, 10L );
			assertNotNull( team );
			assertEquals( 2, team.players.size(), "At revRecReplace, team should have 2 players" );
			var names = team.players.stream().map( p -> p.name ).sorted().toList();
			assertEquals( List.of( "Rec Bob", "Rec Charlie" ), names );
		}
	}

	// --- ALL_REVISIONS collection isolation ---

	@Test
	@Order(5)
	void testCollectionRevisionIsolation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();
		// Use separate point-in-time sessions within an ALL_REVISIONS session
		// to verify collection isolation across revisions
		try (var s = sf.withOptions().atChangeset( AuditLog.ALL_REVISIONS ).openSession()) {
			var teams = s.createSelectionQuery( "from Team where id = :id", Team.class )
					.setParameter( "id", 1L )
					.getResultList();
			// revCreate + revAdd + revRemove + revDelete(DEL) = 4 revisions
			assertEquals( 4, teams.size(), "Expected 4 revisions including DEL" );

			// Find the revision with 2 players (revAdd) and one of the single-player revisions
			Team teamWith2 = null;
			Team teamWith1 = null;
			for ( var t : teams ) {
				int size = t.players.size();
				if ( size == 2 && teamWith2 == null ) {
					teamWith2 = t;
				}
				else if ( size == 1 && teamWith1 == null ) {
					teamWith1 = t;
				}
			}
			assertNotNull( teamWith2, "Should find a revision with 2 players" );
			assertNotNull( teamWith1, "Should find a revision with 1 player" );

			// Collections must be distinct instances across revisions
			assertNotSame( teamWith1.players, teamWith2.players,
					"Collections at different revisions must not be the same instance" );

			// Verify contents
			assertEquals( 2, teamWith2.players.size() );
			assertEquals( 1, teamWith1.players.size() );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Team")
	static class Team {
		@Id
		long id;
		String name;
		@OneToMany
		@JoinTable
		List<Player> players = new ArrayList<>();

		Team() {
		}

		Team(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Player")
	static class Player {
		@Id
		long id;
		String name;

		Player() {
		}

		Player(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
