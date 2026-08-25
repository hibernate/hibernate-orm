/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loaders;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a collection custom loader defined in {@code mapping.xml} via
 * {@code <hql-select/>} is applied, mirroring the
 * {@link org.hibernate.annotations.HQLSelect} annotation.
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/loaders/collection-custom-select.xml")
@SessionFactory
@Jira( "HHH-20812" )
public class CollectionCustomSelectXmlTest {

	@Test
	public void testCustomCollectionLoader(SessionFactoryScope scope) {
		final Team team = new Team( 1L, "Hibernate" );
		for ( long i = 1; i <= 3; i++ ) {
			final Player player = new Player( i, "Player #" + i );
			player.team = team;
			team.players.add( player );
		}

		scope.inTransaction( session -> session.persist( team ) );

		// the custom loader should see all three players
		scope.inTransaction( session -> {
			final Team loaded = session.find( Team.class, 1L );
			assertThat( loaded.players ).hasSize( 3 );
		} );

		// soft-delete one player - the custom loader filters on deleted = false
		scope.inTransaction( session -> {
			final Player player = session.find( Player.class, 1L );
			player.deleted = true;
		} );

		// the custom loader should now filter out the soft-deleted player,
		// which would still be returned by the default collection loader
		scope.inTransaction( session -> {
			final Team loaded = session.find( Team.class, 1L );
			assertThat( loaded.players ).hasSize( 2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	public static class Team {
		private Long id;
		private String name;
		private Set<Player> players = new HashSet<>();

		public Team() {
		}

		public Team(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class Player {
		private Long id;
		private String name;
		private boolean deleted;
		private Team team;

		public Player() {
		}

		public Player(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
