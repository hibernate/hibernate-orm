/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.connection.BaseDataSource;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12197")
@RequiresDialect(H2Dialect.class)
public class ConnectionsReleaseAutoCommitTest extends BaseEntityManagerFunctionalTestCase {

	private ConnectionProviderDecorator connectionProvider;

	private Connection connection;

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();

		String url = Environment.getProperties().getProperty( Environment.URL );

		Properties connectionProps = new Properties();
		connectionProps.put("user", Environment.getProperties().getProperty( Environment.USER ));
		connectionProps.put("password", Environment.getProperties().getProperty( Environment.PASS ));

		BaseDataSource dataSource = new BaseDataSource() {
			@Override
			public Connection getConnection() throws SQLException {
				return DriverManager.getConnection(url, connectionProps);
			}

			@Override
			public Connection getConnection(String username, String password) throws SQLException {
				return DriverManager.getConnection(url, connectionProps);
			}
		};

		connectionProvider = new ConnectionProviderDecorator( dataSource );
		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		return config;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Thing.class,
		};
	}

	@Test
	public void testConnectionAcquisitionCount() throws SQLException {
		connectionProvider.clear();

		doInJPA( this::entityManagerFactory, entityManager -> {
			assertEquals( 1, connectionProvider.getConnectionCount() );
			Thing thing = new Thing();
			thing.setId( 1 );
			entityManager.persist( thing );
			assertEquals( 1, connectionProvider.getConnectionCount() );
		} );

		assertEquals( 1, connectionProvider.getConnectionCount() );
		verify( connectionProvider.connection, times( 1 ) ).close();
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public static class ConnectionProviderDecorator extends UserSuppliedConnectionProviderImpl {

		private final DataSource dataSource;

		private int connectionCount;

		private Connection connection;

		public ConnectionProviderDecorator(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Connection getConnection() throws SQLException {
			connectionCount++;
			connection = spy(dataSource.getConnection());
			return connection;
		}

		@Override
		public void closeConnection(Connection connection) throws SQLException {
			connection.close();
		}

		public int getConnectionCount() {
			return this.connectionCount;
		}

		public void clear() {
			connectionCount = 0;
		}
	}
}
