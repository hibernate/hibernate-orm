/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc.autocommit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(H2Dialect.class)
@RequiresDialect(value = OracleDialect.class)
@RequiresDialect(SQLServerDialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public class SkipAutoCommitTest extends EntityManagerFactoryBasedFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider =
		new PreparedStatementSpyConnectionProvider() {
			@Override
			protected Connection actualConnection() throws SQLException {
				Connection connection = super.actualConnection();
				connection.setAutoCommit( false );
				return connection;
			}
		};

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();

		config.put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE );
		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );

		return config;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() {
		super.cleanupTestData();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			City.class,
		};
	}

	protected Dialect getDialect() {
		return DialectContext.getDialect();
	}

	@Test
	public void test() throws Throwable {
		inTransaction(
				entityManager -> {
					// Moved inside the transaction because the new base class defers the EMF creation w/ respect to the
					// former base class, so the connections used in that process can now only be cleared after the EMF is built
					// Could also override entityManagerFactoryBuilt(EntityManagerFactory factory) and do it there.
					connectionProvider.clear();

					City city = new City();
					city.setId( 1L );
					city.setName( "Cluj-Napoca" );
					entityManager.persist( city );

					assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );
					assertTrue( connectionProvider.getReleasedConnections().isEmpty() );
				}
		);
		verifyConnections();

		connectionProvider.clear();
		inTransaction(
				entityManager -> {
					City city = entityManager.find( City.class, 1L );
					assertEquals( "Cluj-Napoca", city.getName() );
				}
		);
		verifyConnections();
	}

	private void verifyConnections() throws Throwable {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		Connection connection = connections.get( 0 );
		List<Object[]> setAutoCommitCalls = connectionProvider.spyContext.getCalls(
				Connection.class.getMethod( "setAutoCommit", boolean.class ),
				connection
		);
		assertEquals( 0, setAutoCommitCalls.size() );
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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
