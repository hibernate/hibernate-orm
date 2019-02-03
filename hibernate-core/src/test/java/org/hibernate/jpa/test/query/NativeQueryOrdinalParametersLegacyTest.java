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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.spi.NativeQueryImplementor;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
@TestForIssue( jiraKey = "HHH-13246" )
public class NativeQueryOrdinalParametersLegacyTest extends BaseEntityManagerFunctionalTestCase {

	private static final String[] GAME_TITLES = { "Super Mario Brothers", "Mario Kart", "F-Zero" };

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Game.class
		};
	}

	private final int FIRST_PARAMETER_INDEX = 0;

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.JDBC_TYLE_PARAMS_ZERO_BASE, true );
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
	public void testZeroBasedParameterBinding() {
		final String sqlString = "SELECT * FROM GAME g WHERE title = ?";

		doInJPA( this::entityManagerFactory, entityManager -> {
			NativeQuery sqlQuery = entityManager.unwrap( Session.class ).createSQLQuery( sqlString );
			sqlQuery.setParameter( FIRST_PARAMETER_INDEX, "Super Mario Brothers" ).setCacheable( true );

			List results = sqlQuery.list();
			assertEquals( 1, results.size() );

			NativeQueryImplementor query = (NativeQueryImplementor) entityManager.createNativeQuery( sqlString );
			query.setParameter( FIRST_PARAMETER_INDEX, "Super Mario Brothers" );
			List list = query.list();
			assertEquals( 1, list.size() );

			sqlQuery = entityManager.unwrap( Session.class ).createSQLQuery( sqlString );
			sqlQuery.setParameter( FIRST_PARAMETER_INDEX, "Super Mario Brothers" ).setCacheable( true );

			results = sqlQuery.list();
			assertEquals( 1, results.size() );
		} );
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
