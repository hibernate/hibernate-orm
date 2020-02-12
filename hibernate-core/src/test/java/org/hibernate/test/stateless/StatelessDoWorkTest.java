/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stateless;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class StatelessDoWorkTest extends BaseCoreFunctionalTestCase {
	public static final String EXPECTED_ENTITY_NAME = "test";
	public static final Integer PERSISTED_TEST_ENTITY_ID = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					TestEntity entity = new TestEntity( PERSISTED_TEST_ENTITY_ID, EXPECTED_ENTITY_NAME );
					session.save( entity );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from TestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDoReturningWork() {
		String retrievedEntityName;
		try (StatelessSession statelessSession = sessionFactory().openStatelessSession()) {
			retrievedEntityName = statelessSession.doReturningWork(
					(connection) -> {
						try (PreparedStatement preparedStatement = connection.prepareStatement(
								"SELECT NAME FROM TEST_ENTITY WHERE ID = ?" )) {
							preparedStatement.setInt( 1, PERSISTED_TEST_ENTITY_ID );
							ResultSet resultSet = preparedStatement.executeQuery();
							String name = null;
							if ( resultSet.next() ) {
								name = resultSet.getString( 1 );
							}
							return name;
						}
					}
			);
		}

		assertThat( retrievedEntityName, is( EXPECTED_ENTITY_NAME ) );
	}

	@Test
	public void testDoWork() {
		try (StatelessSession statelessSession = sessionFactory().openStatelessSession()) {
			statelessSession.doWork(
					(connection) -> {
						try (PreparedStatement preparedStatement = connection.prepareStatement(
								"DELETE FROM TEST_ENTITY " )) {
							preparedStatement.execute();
						}
					}
			);
		}

		assertThatAllTestEntitiesHaveBeenDeleted();
	}

	private void assertThatAllTestEntitiesHaveBeenDeleted() {
		inTransaction( session -> {
			List results = session.createQuery( "from TestEntity" ).list();
			assertThat( results.size(), is( 0 ) );
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@Column(name = "ID")
		private Integer id;

		@Column(name = "NAME")
		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
