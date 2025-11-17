/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = { NamedQueryCommentTest.Game.class },
		integrationSettings = {
				@Setting( name = AvailableSettings.USE_SQL_COMMENTS, value = "true" ),
				@Setting( name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false" )
		},
		useCollectingStatementInspector = true
)
@JiraKey(value = "HHH-11640")
public class NamedQueryCommentTest {

	private static SQLStatementInspector statementInspector;

	private static final String[] GAME_TITLES = { "Halo", "Grand Theft Auto", "NetHack" };

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {

		statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				entityManager -> {
					for ( String title : GAME_TITLES ) {
						Game game = new Game( title );
						entityManager.persist( game );
					}
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Game" ).executeUpdate()
		);
	}

	@Test
	public void testSelectNamedQueryWithSqlComment(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					TypedQuery<Game> query = entityManager.createNamedQuery( "SelectNamedQuery", Game.class );
					query.setParameter( "title", GAME_TITLES[0] );
					List<Game> list = query.getResultList();
					assertEquals( 1, list.size() );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_SELECT_INDEX_game_title */ select g1_0.id,g1_0.title from game g1_0 where g1_0.title=?"
					);
				}
		);
	}

	@Test
	public void testSelectNamedNativeQueryWithSqlComment(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					TypedQuery<Game> query = entityManager.createNamedQuery( "SelectNamedNativeQuery", Game.class );
					query.setParameter( "title", GAME_TITLES[0] );
					List<Game> list = query.getResultList();
					assertEquals( 1, list.size() );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* + INDEX (game idx_game_title)  */ select * from game g where title = ?"

					);
				}
		);
	}

	@Test
	public void testUpdateNamedQueryWithSqlComment(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
					query.setParameter( "title", GAME_TITLES[0] );
					query.setParameter( "id", 1L );
					int updateCount = query.executeUpdate();
					assertEquals( 1, updateCount );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
					);
				}
		);
	}

	@Test
	public void testUpdateNamedNativeQueryWithSqlComment(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
					query.setParameter( "title", GAME_TITLES[0] );
					query.setParameter( "id", 1L );
					int updateCount = query.executeUpdate();
					assertEquals( 1, updateCount );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
					);
				}
		);
	}

	@Test
	@RequiresDialect(value = OracleDialect.class)
	public void testUpdateNamedNativeQueryWithQueryHintUsingOracle(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
					query.setParameter( "title", GAME_TITLES[0] );
					query.setParameter( "id", 1L );
					query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "INDEX (game idx_game_id)" );
					int updateCount = query.executeUpdate();
					assertEquals( 1, updateCount );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_INDEX_game_title */ update /*+ INDEX (game idx_game_id) */ game set title = ? where id = ?"
					);
				}
		);
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testUpdateNamedNativeQueryWithQueryHintUsingIndex(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
					query.setParameter( "title", GAME_TITLES[0] );
					query.setParameter( "id", 1L );
					query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "INDEX (game idx_game_id)" );
					int updateCount = query.executeUpdate();
					assertEquals( 1, updateCount );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
					);
				}
		);
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(H2Dialect.class)
	public void testSelectNamedNativeQueryWithQueryHintUsingIndex(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();

					Query query = entityManager.createNamedQuery( "SelectNamedQuery" );
					query.setParameter( "title", GAME_TITLES[0] );
					query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "idx_game_id" );
					List<Game> list = query.getResultList();
					assertEquals( 1, list.size() );

					statementInspector.assertExecutedCount(1);

					statementInspector.assertExecuted(
							"/* COMMENT_SELECT_INDEX_game_title */ select g1_0.id,g1_0.title from game g1_0  use index (idx_game_id) where g1_0.title=?"
					);
				}
		);
	}

	@Entity(name = "Game")
	@Table(
			name = "game",
			indexes = {
					@Index(name = "idx_game_title", columnList = "title"),
					@Index(name = "idx_game_id", columnList = "id")
			}
	)
	@NamedQuery(
			name = "SelectNamedQuery",
			query = "select g from Game g where title = :title",
			comment = "COMMENT_SELECT_INDEX_game_title"
	)
	@NamedQuery(
			name = "UpdateNamedQuery",
			query = "update Game set title = :title where id = :id",
			comment = "INDEX (game idx_game_title) "
	)
	@NamedNativeQuery(
			name = "SelectNamedNativeQuery",
			query = "select * from game g where title = :title",
			comment = "+ INDEX (game idx_game_title) ",
			resultClass = Game.class
	)
	@NamedNativeQuery(
			name = "UpdateNamedNativeQuery",
			query = "update game set title = :title where id = :id",
			comment = "COMMENT_INDEX_game_title",
			resultClass = Game.class
	)
	public static class Game {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public Game() {
		}

		public Game(String title) {
			this.title = title;
		}

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
