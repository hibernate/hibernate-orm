/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.PreparedStatement;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresDialect(H2Dialect.class)
@JiraKey( value = "HHH-15403")
public class StoreProcedureStatementsClosedTest extends BaseSessionFactoryFunctionalTest {

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
	);

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SimpleEntity.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		ConnectionProvider connectionProvider = (ConnectionProvider) builer.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER );
		this.connectionProvider.setConnectionProvider( connectionProvider );
		builer.applySetting( AvailableSettings.CONNECTION_PROVIDER, this.connectionProvider );
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session ->
						session.createNativeQuery(
										"CREATE ALIAS " + MyStoredProcedure.NAME + " FOR \"" + MyStoredProcedure.class.getName() + ".execute\"" )
								.executeUpdate()
		);
		inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity( "initial name" );
					entity.setId( 1L );
					session.persist( entity );
				}
		);
	}

	@AfterAll
	public void tearDown() {
		inTransaction(
				session ->
						session.createNativeQuery( "DROP ALIAS " + MyStoredProcedure.NAME ).executeUpdate()
		);
		connectionProvider.stop();
	}

	@Test
	public void testIt() throws Exception {
		inTransaction(
				session -> {
					StoredProcedureQuery storedProcedure = session.createStoredProcedureQuery( MyStoredProcedure.NAME );
					storedProcedure.registerStoredProcedureParameter( 0, Long.class, ParameterMode.IN );
					storedProcedure.setParameter( 0, 1L );
					storedProcedure.execute();
					storedProcedure.getSingleResult();
				}
		);

		for ( PreparedStatement statement : connectionProvider.getPreparedStatements() ) {
			assertTrue( statement.isClosed() );
		}
	}

	public static class MyStoredProcedure {
		private static final String NAME = "myStoredProc";
		private static final String RESULT_PREFIX = "StoredProcResult";

		@SuppressWarnings("unused")
		public static String execute(long id) {
			return RESULT_PREFIX + id;
		}
	}

	@Entity(name = "SimpleEntity")
	public class SimpleEntity {

		@Id
		private long id;

		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(String name) {
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
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
