/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11092")
@Jpa(annotatedClasses = {NamedQueryTest.Game.class})
public class NamedQueryTest {

	private static final String[] GAME_TITLES = { "Halo", "Grand Theft Auto", "NetHack" };

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope)
			throws Exception {
		scope.inTransaction( entityManager -> {
			for ( String title : GAME_TITLES ) {
				Game game = new Game( title );
				entityManager.persist( game );
			}
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					entityManager.createQuery( "delete from Game" ).executeUpdate();
				}
		);
	}

	@Test
	public void testNamedQueriesOrdinalParametersAreOneBased(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					Query query = entityManager.createNamedQuery( "NamedQuery" );
					query.setParameter( 1, GAME_TITLES[0] );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testNamedQueryOrdinalParametersConflict(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					Query query = entityManager.createNamedQuery( "NamedQuery" );
					query.setParameter( 1, GAME_TITLES[0] );
					List list = query.getResultList();
					assertEquals( 1, list.size() );

					final Session session = entityManager.unwrap( Session.class );
					final org.hibernate.query.Query sessionQuery = session.createQuery( "select g from Game g where title = ?1" );
					sessionQuery.setParameter( 1, GAME_TITLES[0] );
					list = sessionQuery.getResultList();

					query.setParameter( 1, GAME_TITLES[0] );
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testNamedQueryOrdinalParametersConflict2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
				}
		);
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersAreOneBased(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					Query query = entityManager.createNamedQuery( "NamedNativeQuery" );
					query.setParameter( 1, GAME_TITLES[0] );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersConflict(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
				}
		);
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersConflict2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12621")
	public void testNativeQueriesFromNamedQueriesDoNotShareQuerySpaces(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

	@Test
	@JiraKey(value = "HHH-11413")
	public void testNamedNativeQueryExceptionNoResultDefined(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createNamedQuery( "NamedNativeQuery", Game.class ),
					"Named query exists but its result type is not compatible"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-11413")
	public void testNamedQueryAddedFromTypedNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery(
					"select g.title from Game g where title = ?", String.class );
			scope.getEntityManagerFactory().addNamedQuery( "the-query", query );

			final TypedQuery<String> namedQuery = entityManager.createNamedQuery( "the-query", String.class );
			namedQuery.setParameter( 1, "abc" );
			namedQuery.getResultList();
		} );
	}

	@Test
	@JiraKey("HHH-17566")
	public void testNamedQueryAddedFromEntityNativeQuery(EntityManagerFactoryScope scope) {
		// Check that the native query works
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery(
					"select g.* from Game g where title = ?", Game.class );
			query.setParameter( 1, "Halo" );
			assertThat( (List<?>) query.getResultList() )
					.hasSize( 1 )
					.element( 0 )
					.asInstanceOf( InstanceOfAssertFactories.type( Game.class ) )
					.returns( "Halo", Game::getTitle );
		} );
		// Check corresponding named query can be used as a typed query
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery(
					"select g.* from Game g where title = ?", Game.class );
			scope.getEntityManagerFactory().addNamedQuery( "the-query", query );

			final TypedQuery<Game> namedQuery = entityManager.createNamedQuery( "the-query", Game.class );
			namedQuery.setParameter( 1, "Halo" );
			assertThat( namedQuery.getResultList() )
					.hasSize( 1 )
					.element( 0 )
					.returns( "Halo", Game::getTitle );
		} );
	}

	@Test
	@JiraKey("HHH-17566")
	public void testNamedQueryAddedFromEntityNativeQueryUsedAsUntyped(EntityManagerFactoryScope scope) {
		// Check corresponding named query can be used as an untyped query
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery(
					"select g.* from Game g where title = ?", Game.class );
			query.setParameter( 1, "Halo" );
			assertThat( (List<?>) query.getResultList() )
					.hasSize( 1 )
					.element( 0 )
					.asInstanceOf( InstanceOfAssertFactories.type( Game.class ) )
					.returns( "Halo", Game::getTitle );
		} );
		// Check naming the native query works
		scope.inTransaction( entityManager -> {
			final Query query = entityManager.createNativeQuery(
					"select g.* from Game g where title = ?", Game.class );
			scope.getEntityManagerFactory().addNamedQuery( "the-query", query );

			final Query namedQuery = entityManager.createNamedQuery( "the-query" );
			namedQuery.setParameter( 1, "Halo" );
			assertThat( (List<?>) namedQuery.getResultList() )
					.hasSize( 1 )
					.element( 0 )
					.asInstanceOf( InstanceOfAssertFactories.type( Game.class ) )
					.returns( "Halo", Game::getTitle );
		} );
	}

	@Entity(name = "Game")
	@NamedQueries(@NamedQuery(name = "NamedQuery", query = "select g from Game g where title = ?1"))
	@NamedNativeQueries(@NamedNativeQuery(name = "NamedNativeQuery", query = "select * from Game g where title = ?"))
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

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
