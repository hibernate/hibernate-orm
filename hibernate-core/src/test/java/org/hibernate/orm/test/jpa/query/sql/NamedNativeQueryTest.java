/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query.sql;

import org.hibernate.Session;

import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;

import org.hibernate.query.NativeQuery;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Query;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11092")
@Disabled("Native queries are not yet implemented in ORM 6")
public class NamedNativeQueryTest extends EntityManagerFactoryBasedFunctionalTest {

	private static final String[] GAME_TITLES = { "Halo", "Grand Theft Auto", "NetHack" };

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Game.class };
	}

	@BeforeEach
	public void setUp() {
		inTransaction( entityManager -> {
			for ( String title : GAME_TITLES ) {
				Game game = new Game( title );
				entityManager.persist( game );
			}
		} );
	}

	@AfterEach
	public void tearDown() {
		inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Game" ).executeUpdate();
		} );
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersAreOneBased() {
		inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedNativeQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		} );
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersConflict() {
		inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedNativeQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );

			final Session session = entityManager.unwrap( Session.class );
			final org.hibernate.query.Query sessionQuery = session.createNativeQuery(
					"select * from Game g where title = ?" );
			sessionQuery.setParameter( 1, GAME_TITLES[0] );
			list = sessionQuery.getResultList();

			query.setParameter( 1, GAME_TITLES[0] );
			assertEquals( 1, list.size() );
		} );
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersConflict2() {
		inTransaction( entityManager -> {
			Query query = entityManager.createNamedQuery( "NamedNativeQuery" );
			query.setParameter( 1, GAME_TITLES[0] );
			List list = query.getResultList();
			assertEquals( 1, list.size() );

			final Session session = entityManager.unwrap( Session.class );
			final org.hibernate.query.Query sessionQuery = session.getNamedNativeQuery(
					"NamedNativeQuery" );
			sessionQuery.setParameter( 1, GAME_TITLES[0] );
			list = sessionQuery.getResultList();

			query.setParameter( 1, GAME_TITLES[0] );
			assertEquals( 1, list.size() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12621")
	public void testNativeQueriesFromNamedQueriesDoNotShareQuerySpaces() {
		inTransaction( entityManager -> {
			Query originalQuery = entityManager.createNativeQuery( "select g from Game g where title = ?1" );
			entityManager.getEntityManagerFactory().addNamedQuery( "myQuery", originalQuery );

			NativeQuery<?> query1 = entityManager.createNamedQuery( "myQuery" ).unwrap( NativeQuery.class );
			query1.addSynchronizedQuerySpace( "newQuerySpace" );

			assertEquals( 1, query1.getSynchronizedQuerySpaces().size() );
			assertEquals( "newQuerySpace", query1.getSynchronizedQuerySpaces().iterator().next() );

			NativeQuery<?> query2 = entityManager.createNamedQuery( "myQuery" ).unwrap( NativeQuery.class );

			assertEquals( 0, query2.getSynchronizedQuerySpaces().size() );
		} );
	}

	@Entity(name = "Game")
	@NamedNativeQueries(@NamedNativeQuery(name = "NamedNativeQuery", query = "select * from Game g where title = ?"))
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
