/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.spi.NativeQueryImplementor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
public class NativeQueryOrdinalParametersTest extends BaseEntityManagerFunctionalTestCase {

	private static final String[] GAME_TITLES = { "Super Mario Brothers", "Mario Kart", "F-Zero" };

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Game.class};
	}

	@Before
	public void setUp(){
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			for ( String title : GAME_TITLES ) {
				Game game = new Game( title );
				em.persist( game );
			}
			em.getTransaction().commit();
		}catch (Exception e){
			if(em.getTransaction().isActive()){
				em.getTransaction().rollback();
			}
			throw e;
		}finally {
			em.close();
		}
	}

	@After
	public void tearDown(){
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			em.createQuery( "delete from Game" ).executeUpdate();
			em.getTransaction().commit();
		}catch (Exception e){
			if(em.getTransaction().isActive()){
				em.getTransaction().rollback();
			}
			throw e;
		}finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10885")
	public void testNativeQueryIndexedOrdinalParameter() {
		EntityManager em = getOrCreateEntityManager();
		try {
			Query query = em.createNativeQuery( "SELECT * FROM Game g WHERE title = ?1" );
			query.setParameter( 1, "Super Mario Brothers" );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10885")
	public void testNativeQueryOrdinalParameter() {
		EntityManager em = getOrCreateEntityManager();
		try {
			Query query = em.createNativeQuery( "SELECT * FROM Game g WHERE title = ?" );
			query.setParameter( 1, "Super Mario Brothers" );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11121")
	public void testConflictWithSessionNativeQuery(){
		EntityManager em = getOrCreateEntityManager();
		final String sqlString = "SELECT * FROM Game g WHERE title = ?";
		try {
			NativeQuery sqlQuery = em.unwrap( Session.class ).createSQLQuery( sqlString );
			sqlQuery.setString( 0, "Super Mario Brothers").setCacheable( true );

			List results = sqlQuery.list();
			assertEquals( 1, results.size() );

			NativeQueryImplementor query = (NativeQueryImplementor) em.createNativeQuery( sqlString );
			query.setString( 1, "Super Mario Brothers" );
			List list = query.list();
			assertEquals( 1, list.size() );

			sqlQuery = em.unwrap( Session.class ).createSQLQuery( sqlString );
			sqlQuery.setString( 0, "Super Mario Brothers").setCacheable( true );

			results = sqlQuery.list();
			assertEquals( 1, results.size() );

			query.setString( 1, "Super Mario Brothers" );
		}
		finally {
			em.close();
		}
	}

	@Entity(name = "Game")
	@Table(name = "GAME")
	public static class Game {
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

		@NotNull
		@Size(min = 3, max = 50)
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
