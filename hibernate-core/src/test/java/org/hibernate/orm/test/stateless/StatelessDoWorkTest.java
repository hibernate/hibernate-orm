/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = StatelessDoWorkTest.TestEntity.class
)
public class StatelessDoWorkTest {
	public static final String EXPECTED_ENTITY_NAME = "test";
	public static final Integer PERSISTED_TEST_ENTITY_ID = 1;


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( PERSISTED_TEST_ENTITY_ID, EXPECTED_ENTITY_NAME );
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SessionFactory
	public void testDoReturningWork(SessionFactoryScope scope) {
		String retrievedEntityName;
		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
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
	@SessionFactory
	public void testDoWork(SessionFactoryScope scope) {
		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
			statelessSession.doWork(
					(connection) -> {
						try (PreparedStatement preparedStatement = connection.prepareStatement(
								"DELETE FROM TEST_ENTITY " )) {
							preparedStatement.execute();
						}
					}
			);
		}

		assertThatAllTestEntitiesHaveBeenDeleted( scope );
	}

	@Test
	@SessionFactory(useCollectingStatementInspector = true)
	public void testStatelessSessionWithStatementInspector(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inStatelessSession(
				session -> {
					session.createQuery( "from TestEntity", TestEntity.class ).list();
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Test
	@SessionFactory
	public void testStatelessSessionWithStatementInspector2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = new SQLStatementInspector();
		final StatelessSessionBuilder statelessSessionBuilder = scope.getSessionFactory().withStatelessOptions().statementInspector( statementInspector );
		StatelessSession session = statelessSessionBuilder.openStatelessSession();
		session.createQuery( "from TestEntity", TestEntity.class ).list();
		statementInspector.assertExecutedCount( 1 );
		statementInspector.clear();
		session.close();
	}

	private void assertThatAllTestEntitiesHaveBeenDeleted(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
