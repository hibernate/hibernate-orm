/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = NamedQueryTest.Game.class
)
@SessionFactory
public class NamedQueryTest {

	private static final String[] GAME_TITLES = { "Halo", "Grand Theft Auto", "NetHack" };


	@BeforeEach
	public void setUp(SessionFactoryScope scope)
			throws Exception {
		scope.inTransaction(
				session -> {
					for ( String title : GAME_TITLES ) {
						Game game = new Game( title );
						session.persist( game );
					}
				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNamedQueriesOrdinalParametersAreOneBased(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.getNamedQuery( "NamedQuery" );
					query.setParameter( 1, GAME_TITLES[0] );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15263" )
	public void testNoExceptionThrownForNamedUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.getNamedQuery( "NamedUpdate" );
					query.setParameter( 1, GAME_TITLES[0] + " 2" );
					query.setParameter( 2, GAME_TITLES[0] );
					assertDoesNotThrow( () -> query.executeUpdate(), "without fixing, 'java.lang.IllegalStateException: Expecting a SELECT query' exception would be thrown" );
				}
		);
	}

	@Test
	public void testNativeNamedQueriesOrdinalParametersAreOneBased(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.getNamedNativeQuery( "NamedNativeQuery" );
					query.setParameter( 1, GAME_TITLES[0] );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Entity(name = "Game")
	@NamedQueries({
			@NamedQuery(name = "NamedQuery", query = "select g from Game g where title = ?1"),
			@NamedQuery(name = "NamedUpdate", query = "update Game set title = ?1 where title = ?2")
	})
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
