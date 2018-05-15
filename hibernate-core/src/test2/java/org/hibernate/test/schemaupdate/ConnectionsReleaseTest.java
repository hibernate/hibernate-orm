/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10443")
public class ConnectionsReleaseTest extends BaseUnitTestCase {

	public static Properties getConnectionProviderProperties() {
		Properties props = new Properties();
		props.put( Environment.DRIVER, "org.h2.Driver" );
		props.put( Environment.URL, String.format( "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", "db1" ) );
		props.put( Environment.USER, "sa" );
		props.put( Environment.PASS, "" );
		return props;
	}

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	private ConnectionProviderDecorator connectionProvider;

	@Before
	public void setUp() {
		connectionProvider = new ConnectionProviderDecorator();
		connectionProvider.configure( getConnectionProviderProperties() );

		ssr = new StandardServiceRegistryBuilder()
				.addService( ConnectionProvider.class, connectionProvider )
				.applySetting(Environment.DIALECT, H2Dialect.class.getName())
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Thing.class )
				.buildMetadata();
		metadata.validate();
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaUpdateReleasesAllConnections() throws SQLException {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		assertThat( connectionProvider.getOpenConnection(), is( 0 ) );
	}

	@Test
	public void testSchemaValidatorReleasesAllConnections() throws SQLException {
		new SchemaValidator().validate( metadata );
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
