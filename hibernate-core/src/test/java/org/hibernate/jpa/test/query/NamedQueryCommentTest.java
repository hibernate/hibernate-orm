/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11640")
public class NamedQueryCommentTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
		options.put( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
	}

	private static final String[] GAME_TITLES = { "Halo", "Grand Theft Auto", "NetHack" };

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Game.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( String title : GAME_TITLES ) {
				Game game = new Game( title );
				entityManager.persist( game );
			}
		} );
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete from Game" ).executeUpdate();
		} );
	}

	@Test
	public void testSelectNamedQueryWithSqlComment() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			TypedQuery<Game> query = entityManager.createNamedQuery( "SelectNamedQuery", Game.class );
			query.setParameter( "title", GAME_TITLES[0] );
			List<Game> list = query.getResultList();
			assertEquals( 1, list.size() );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
				"/* COMMENT_SELECT_INDEX_game_title */ select namedquery0_.id as id1_0_, namedquery0_.title as title2_0_ from game namedquery0_ where namedquery0_.title=?"
			);
		} );
	}

	@Test
	public void testSelectNamedNativeQueryWithSqlComment() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			TypedQuery<Game> query = entityManager.createNamedQuery( "SelectNamedNativeQuery", Game.class );
			query.setParameter( "title", GAME_TITLES[0] );
			List<Game> list = query.getResultList();
			assertEquals( 1, list.size() );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* + INDEX (game idx_game_title)  */ select * from game g where title = ?"

			);
		} );
	}

	@Test
	public void testUpdateNamedQueryWithSqlComment() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
			query.setParameter( "title", GAME_TITLES[0] );
			query.setParameter( "id", 1L );
			int updateCount = query.executeUpdate();
			assertEquals( 1, updateCount );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
			);
		} );
	}

	@Test
	public void testUpdateNamedNativeQueryWithSqlComment() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
			query.setParameter( "title", GAME_TITLES[0] );
			query.setParameter( "id", 1L );
			int updateCount = query.executeUpdate();
			assertEquals( 1, updateCount );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
			);
		} );
	}

	@Test
	@RequiresDialect(Oracle8iDialect.class)
	public void testUpdateNamedNativeQueryWithQueryHintUsingOracle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
			query.setParameter( "title", GAME_TITLES[0] );
			query.setParameter( "id", 1L );
			query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "INDEX (game idx_game_id)" );
			int updateCount = query.executeUpdate();
			assertEquals( 1, updateCount );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* COMMENT_INDEX_game_title */ update /*+ INDEX (game idx_game_id) */ game set title = ? where id = ?"
			);
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testUpdateNamedNativeQueryWithQueryHintUsingIndex() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			Query query = entityManager.createNamedQuery( "UpdateNamedNativeQuery" );
			query.setParameter( "title", GAME_TITLES[0] );
			query.setParameter( "id", 1L );
			query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "INDEX (game idx_game_id)" );
			int updateCount = query.executeUpdate();
			assertEquals( 1, updateCount );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* COMMENT_INDEX_game_title */ update game set title = ? where id = ?"
			);
		} );
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(H2Dialect.class)
	public void testSelectNamedNativeQueryWithQueryHintUsingIndex() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			sqlStatementInterceptor.clear();

			Query query = entityManager.createNamedQuery( "SelectNamedQuery" );
			query.setParameter( "title", GAME_TITLES[0] );
			query.unwrap( org.hibernate.query.Query.class ).addQueryHint( "idx_game_id" );
			List<Game> list = query.getResultList();
			assertEquals( 1, list.size() );

			sqlStatementInterceptor.assertExecutedCount(1);

			sqlStatementInterceptor.assertExecuted(
					"/* COMMENT_SELECT_INDEX_game_title */ select namedquery0_.id as id1_0_, namedquery0_.title as title2_0_ from game namedquery0_  USE INDEX (idx_game_id) where namedquery0_.title=?"			)
			;
		} );
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
