/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query.hql;

import org.hibernate.Session;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11092")
public class NamedQueryTest extends EntityManagerFactoryBasedFunctionalTest {

	private static final String[] GAME_TITLES = {"Halo", "Grand Theft Auto", "NetHack"};

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Game.class };
	}

	@BeforeEach
	public void setUp() {
		entityManagerFactoryScope().inTransaction( entityManager -> {
			for ( String title : GAME_TITLES ) {
				Game game = new Game( title );
				entityManager.persist( game );
			}
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testNamedQueriesOrdinalParametersAreOneBased() {
		entityManagerFactoryScope().inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		} );
	}

	@Test
	public void testNamedQueryOrdinalParametersConflict() {
		entityManagerFactoryScope().inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );

			final Session session = entityManager.unwrap( Session.class );
			final org.hibernate.query.Query sessionQuery = session.createQuery(
					"select g from Game g where title = ?1" );
			sessionQuery.setParameter( 1, GAME_TITLES[0] );
			list = sessionQuery.getResultList();

			query.setParameter( 1, GAME_TITLES[0] );
			assertEquals( 1, list.size() );
		} );
	}

	@Test
	public void testNamedQueryOrdinalParametersConflict2() {
		entityManagerFactoryScope().inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );

			final Session session = entityManager.unwrap( Session.class );
			final org.hibernate.query.Query sessionQuery = session.getNamedQuery( "NamedQuery" );
			sessionQuery.setParameter( 1, GAME_TITLES[0] );
			list = sessionQuery.getResultList();

			query.setParameter( 1, GAME_TITLES[0] );
			assertEquals( 1, list.size() );
		} );
	}

	@Entity(name = "Game")
	@NamedQueries(@NamedQuery(name = "NamedQuery", query = "select g from Game g where title = ?1"))
	static class Game {
		private Long id;
		private String title;

		public Game() {
		}

		public Game(String title) {
			this.title = title;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
