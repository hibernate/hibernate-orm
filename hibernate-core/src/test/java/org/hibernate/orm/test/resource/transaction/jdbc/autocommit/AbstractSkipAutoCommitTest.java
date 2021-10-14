/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.resource.transaction.jdbc.autocommit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public abstract class AbstractSkipAutoCommitTest extends EntityManagerFactoryBasedFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider =
		new PreparedStatementSpyConnectionProvider( false, true ) {
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

		config.put( AvailableSettings.DATASOURCE, dataSource() );
		config.put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE );
		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );

		return config;
	}

	protected abstract DataSource dataSource();

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
	public void test() {
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

	private void verifyConnections() {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		Connection connection = connections.get( 0 );
		try {
			verify(connection, never()).setAutoCommit( false );
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
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
