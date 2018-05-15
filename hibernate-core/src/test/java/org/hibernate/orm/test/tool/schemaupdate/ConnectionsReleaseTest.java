/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10443")
public class ConnectionsReleaseTest extends BaseSchemaUnitTestCase {

	public static Properties getConnectionProviderProperties() {
		Properties props = new Properties();
		props.put( Environment.DRIVER, "org.h2.Driver" );
		props.put( Environment.URL, String.format( "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", "db1" ) );
		props.put( Environment.USER, "sa" );
		props.put( Environment.PASS, "" );
		return props;
	}

	private ConnectionProviderDecorator connectionProvider;

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		connectionProvider = new ConnectionProviderDecorator();
		connectionProvider.configure( getConnectionProviderProperties() );
		serviceRegistryBuilder.addService( ConnectionProvider.class, connectionProvider )
				.applySetting( Environment.DIALECT, H2Dialect.class.getName() );
	}

	@SchemaTest
	public void testSchemaUpdateReleasesAllConnections(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate.execute( EnumSet.of( TargetType.DATABASE ) ) );
		assertThat( connectionProvider.getOpenConnection(), is( 0 ) );
	}

	@SchemaTest
	public void testSchemaValidatorReleasesAllConnections(SchemaScope scope) {
		scope.withSchemaValidator( schemaValidator ->
										   schemaValidator.validate() );
		assertThat( connectionProvider.getOpenConnection(), is( 0 ) );
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;
	}

	public static class ConnectionProviderDecorator extends DriverManagerConnectionProviderImpl {
		private int openConnection;

		@Override
		public Connection getConnection() throws SQLException {
			openConnection++;
			return super.getConnection();
		}

		@Override
		public void closeConnection(Connection conn) throws SQLException {
			super.closeConnection( conn );
			openConnection--;
		}

		public int getOpenConnection() {
			return this.openConnection;
		}
	}
}
