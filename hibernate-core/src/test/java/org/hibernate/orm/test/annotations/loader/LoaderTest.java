/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = { Player.class, Team.class } )
@SessionFactory
public class LoaderTest {
	@Test
	public void testBasic(SessionFactoryScope sessions) throws Exception {
		// set up data...
		sessions.inTransaction( (session) -> {
			Team t = new Team( 1L );
			Player p = new Player( 1L, "me" );
			t.addPlayer( p );
			session.persist( p );
			session.persist( t );
		} );

		// test
		sessions.inTransaction( (session) -> {
			Team t2 = session.getReference( Team.class, 1 );
			Set<Player> players = t2.getPlayers();
			Iterator<Player> iterator = players.iterator();
			assertThat( iterator.next().getName() ).isEqualTo( "me" );
		} );
	}

	@Test
	public void testGetNotExisting(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Team reference = session.getReference( Team.class, 1 );
			assertThat( reference ).isNotNull();

			// now try a find which should return us a null
			try {
				final Team found = session.find( Team.class, 1 );
				assertThat( found ).isNull();
			}
			catch (ObjectNotFoundException unexpected) {
				fail( "#find threw an ObjectNotFoundException" );
			}
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}
}
