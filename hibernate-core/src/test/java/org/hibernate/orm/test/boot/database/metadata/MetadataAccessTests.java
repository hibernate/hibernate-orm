/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.database.metadata;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.env.TestingDatabaseInfo;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-17269" )
public class MetadataAccessTests {

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
	}

	@Test
	void testAccessDisabledExplicitDialect() {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, "org.hibernate.dialect.OracleDialect" );
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.JAKARTA_HBM2DDL_DB_NAME );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			assertThat( dialect ).isInstanceOf( OracleDialect.class );
		}
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-18079")
	void testAccessDisabledExplicitProductName() {
		final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.clearSettings();

		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.JAKARTA_HBM2DDL_DB_NAME, "Oracle" );
		assertThat( registryBuilder.getSettings() )
				.doesNotContainKeys( JdbcSettings.DIALECT );

		try (StandardServiceRegistry registry = registryBuilder.build()) {
			final JdbcEnvironment jdbcEnvironment = registry.getService( JdbcEnvironment.class );
			final Dialect dialect = jdbcEnvironment.getDialect();
			assertThat( dialect ).isInstanceOf( OracleDialect.class );
		}
	}

	@Test
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
}
