/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.database.metadata;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.env.TestingDatabaseInfo;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.jboss.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-17269" )
public class MetadataAccessTests {

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension
			.builder().setLogger(
					Logger.getMessageLogger( CoreMessageLogger.class, Dialect.class.getName() )
			).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages( "HHH000511" );
		triggerable.reset();
	}

	@Test
	void testAccessAllowed() {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		// allow access to the jdbc metadata
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, true );

		// configure the values needed to connect to a H2 database
		registryBuilder.applySetting( AvailableSettings.JAKARTA_JDBC_DRIVER, TestingDatabaseInfo.DRIVER );
		registryBuilder.applySetting( AvailableSettings.JAKARTA_JDBC_URL, TestingDatabaseInfo.URL );
		registryBuilder.applySetting( AvailableSettings.JAKARTA_JDBC_USER, TestingDatabaseInfo.USER );
		registryBuilder.applySetting( AvailableSettings.JAKARTA_JDBC_PASSWORD, TestingDatabaseInfo.PASS );

		// make certain there is no explicit dialect configured
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.DIALECT, JdbcSettings.JAKARTA_HBM2DDL_DB_NAME );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			assertThat( dialect ).isNotNull();
			assertThat( dialect ).isInstanceOf( H2Dialect.class );
		}

		assertThat( triggerable.triggerMessages() )
				.as( triggerable.toString() )
				.isEmpty();
	}

	static Stream<Arguments> dialects() {
		return Stream.of(
				Arguments.of( "DB2", DB2Dialect.class,
						getVersionConstant( DB2Dialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "Apache Derby", DerbyDialect.class,
						getVersionConstant( DerbyDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "EnterpriseDB", PostgresPlusDialect.class,
						getVersionConstant( PostgreSQLDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "H2", H2Dialect.class,
						getVersionConstant( H2Dialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "HSQL Database Engine", HSQLDialect.class,
						getVersionConstant( HSQLDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "HDB", HANADialect.class,
						getVersionConstant( HANADialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "MariaDB", MariaDBDialect.class,
						getVersionConstant( MariaDBDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "MySQL", MySQLDialect.class,
						getVersionConstant( MySQLDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "Oracle", OracleDialect.class,
						getVersionConstant( OracleDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "PostgreSQL", PostgreSQLDialect.class,
						getVersionConstant( PostgreSQLDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "Google Cloud Spanner", SpannerDialect.class, ZERO_VERSION ),
				Arguments.of( "Microsoft SQL Server", SQLServerDialect.class,
						getVersionConstant( SQLServerDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "Sybase SQL Server", SybaseDialect.class,
						getVersionConstant( SybaseDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "Adaptive Server Enterprise", SybaseDialect.class,
						getVersionConstant( SybaseDialect.class, "MINIMUM_VERSION") ),
				Arguments.of( "ASE", SybaseDialect.class,
						getVersionConstant( SybaseDialect.class, "MINIMUM_VERSION") )
		);
	}

	@ParameterizedTest
	@MethodSource("dialects")
	void testAccessDisabledExplicitDialect(String productName, Class<?> dialectClass, DatabaseVersion expectedDatabaseVersion) {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, dialectClass.getName() );
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.JAKARTA_HBM2DDL_DB_NAME );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			assertThat( dialect ).isInstanceOf( dialectClass );
			assertThat( dialect.getVersion() ).isEqualTo( expectedDatabaseVersion );
		}

		assertThat( triggerable.triggerMessages() )
				.as( triggerable.toString() )
				.isEmpty();
	}

	@ParameterizedTest
	@MethodSource("dialects")
	@Jira("https://hibernate.atlassian.net/browse/HHH-18079")
	@Jira("https://hibernate.atlassian.net/browse/HHH-18080")
	void testAccessDisabledExplicitProductName(String productName, Class<?> dialectClass, DatabaseVersion expectedDatabaseVersion) {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.JAKARTA_HBM2DDL_DB_NAME, productName );
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.DIALECT );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			assertThat( dialect ).isInstanceOf( dialectClass );
			assertThat( dialect.getVersion() ).isEqualTo( expectedDatabaseVersion );
		}

		assertThat( triggerable.triggerMessages() )
				.as( triggerable.toString() )
				.isEmpty();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-18080")
	void testAccessDisabledNoDialectNorProductName() {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.DIALECT, JdbcSettings.JAKARTA_HBM2DDL_DB_NAME );

		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			fail( "Should fail to boot - " + dialect );
		}
		catch (ServiceException expected) {
			assertThat( expected.getCause() ).isInstanceOf( HibernateException.class );
			final HibernateException cause = (HibernateException) expected.getCause();
			assertThat( cause.getMessage() ).startsWith( "Unable to determine Dialect without JDBC metadata" );
		}
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-18286")
	void testDontIgnoreMetadataAccessFailureWhenConnectionCantBeObtained() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		registryBuilder.applySetting( JdbcSettings.IGNORE_METADATA_ACCESS_FAILURE_ON_BOOT, false );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			assertThatExceptionOfType( ServiceException.class )
					.isThrownBy( () -> registry.getService( JdbcEnvironment.class ) )
					.havingCause()
					.isInstanceOf( HibernateException.class )
					.withMessage( "Unable to access JDBC metadata" );
		}
	}

	// Ugly hack because neither MINIMUM_VERSION nor getMinimumSupportedVersion()
	// can be accessed from this test.
	private static DatabaseVersion getVersionConstant(Class<? extends Dialect> dialectClass, String versionConstantName) {
		try {
			Field field = dialectClass.getDeclaredField( versionConstantName );
			field.setAccessible( true );
			return (DatabaseVersion) field.get( null );
		}
		catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException( "Error extracting '" + versionConstantName + "' from '" + dialectClass + "'", e );
		}
	}
}
