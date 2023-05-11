package org.hibernate.orm.test.schemaupdate;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-xxxxx")
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSqlBinaryArraySchemaTest {

	private static final Logger log = Logger.getLogger( PostgreSqlBinaryArraySchemaTest.class );

	private static StandardServiceRegistry ssr;

	private Properties getConfig(Object createTarget) {
		final Properties config = Environment.getProperties();
		config.put( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createTarget );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "update" );
		config.put( AvailableSettings.LOADED_CLASSES, List.of( Binaries.class ) );
		return config;
	}

	private static Connection getConnection() throws SQLException {
		return ssr.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess()
				.obtainConnection();
	}

	@BeforeAll
	static void init() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.build();

		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement()) {
			connection.setAutoCommit( true );
			statement.executeUpdate( "drop table if exists binaries cascade" );
			statement.executeUpdate( "create table binaries (id serial not null, bytes bytea, primary key (id))" );
		}
		catch (SQLException e) {
			log.debug( e.getMessage() );
		}
	}

	@AfterAll
	static void destroy() {
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement()) {
			connection.setAutoCommit( true );
			statement.executeUpdate( "drop table if exists binaries cascade" );
		}
		catch (SQLException e) {
			log.debug( e.getMessage() );
		}
		ssr.close();
	}

	@Test
	@JiraKey("HHH-xxxxx")
	void testByteArraySchemaUpdate() {
		final Writer createSchema = new StringWriter();
		EntityManagerFactoryBuilder entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				getConfig( createSchema )
		);

		try {
			entityManagerFactoryBuilder.generateSchema();

			final String[] fileContent = createSchema.toString()
					.toLowerCase()
					.replaceAll( "\\s+", " " ).trim().split( ";" );
			for ( String line : fileContent ) {
				assertFalse(
						line.matches( "alter table.+binaries.+alter.+column bytes.+" ),
						"Column bytes should not be altered"
				);
			}
		}
		finally {
			entityManagerFactoryBuilder.cancel();
		}
	}

	@Entity
	@Table(name = "binaries")
	public static class Binaries {

		@Id
		@GeneratedValue(strategy = IDENTITY)
		@Column(name = "id", nullable = false, updatable = false)
		private Integer id;

		@Column(length = 512)
		private byte[] bytes;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

	}
}
