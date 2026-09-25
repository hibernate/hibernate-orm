/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;

/// Verifies common alias normalization while retaining native and JPA precedence and defaults.
///
/// @author Steve Ebersole
@BaseUnitTest
class BootstrapSettingsNormalizationTest {
	@ParameterizedTest
	@EnumSource(Path.class)
	void resolvedSettingsRetainEntryPointAliasesAndDefaults(Path path) {
		final var input = Map.<String, Object>of(
				URL, "jdbc:canonical",
				JAKARTA_JDBC_URL, "jdbc:canonical",
				DRIVER, "driver", JAKARTA_JDBC_DRIVER, "driver",
				USER, "user", JAKARTA_JDBC_USER, "user",
				PASS, "password", JAKARTA_JDBC_PASSWORD, "password" );
		final var settings = readSettings( path, input );
		assertThat( settings ).containsEntry( URL, "jdbc:canonical" )
				.containsEntry( DRIVER, "driver" ).containsEntry( USER, "user" ).containsEntry( PASS, "password" );
		if ( path.jpa ) {
			assertThat( settings ).containsEntry( JPA_JDBC_URL, "jdbc:canonical" )
					.containsEntry( TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class );
		}
		else {
			assertThat( settings ).doesNotContainKeys( JPA_JDBC_URL, TRANSACTION_COORDINATOR_STRATEGY );
		}
	}

	@ParameterizedTest
	@EnumSource(Path.class)
	void explicitHibernateValuesRetainPrecedence(Path path) {
		final var input = new HashMap<String, Object>( Map.of(
				JPA_JDBC_URL, "jdbc:legacy", JPA_JDBC_DRIVER, "legacy-driver",
				JPA_JDBC_USER, "legacy-user", JPA_JDBC_PASSWORD, "legacy-pass" ) );
		input.putAll( Map.of(
				URL, "jdbc:hibernate", JAKARTA_JDBC_URL, "jdbc:jakarta",
				DRIVER, "hibernate-driver", JAKARTA_JDBC_DRIVER, "jakarta-driver",
				USER, "hibernate-user", JAKARTA_JDBC_USER, "jakarta-user",
				PASS, "hibernate-pass", JAKARTA_JDBC_PASSWORD, "jakarta-pass" ) );
		final var settings = readSettings( path, input );
		assertThat( settings ).containsEntry( URL, "jdbc:hibernate" )
				.containsEntry( DRIVER, "hibernate-driver" ).containsEntry( USER, "hibernate-user" )
				.containsEntry( PASS, "hibernate-pass" );
		assertThat( settings ).containsEntry( JAKARTA_JDBC_URL, path.jpa ? "jdbc:hibernate" : "jdbc:jakarta" );
	}

	@ParameterizedTest
	@EnumSource(value = Path.class, names = { "REGISTRY", "METADATA_SOURCES", "CONFIGURATION" })
	void nativeBootstrapSuppliesOnlyMissingAliases(Path path) {
		final var settings = readSettings( path, Map.of(
				JAKARTA_JDBC_URL, "jdbc:jakarta", JAKARTA_JDBC_DRIVER, "driver",
				JAKARTA_JDBC_USER, "user", JAKARTA_JDBC_PASSWORD, "password" ) );
		assertThat( settings ).containsEntry( URL, "jdbc:jakarta" )
				.containsEntry( DRIVER, "driver" ).containsEntry( USER, "user" ).containsEntry( PASS, "password" )
				.doesNotContainKeys( JPA_JDBC_URL, TRANSACTION_COORDINATOR_STRATEGY );
	}

	@ParameterizedTest
	@EnumSource(value = Path.class, names = { "REGISTRY", "METADATA_SOURCES", "CONFIGURATION" })
	void legacyJdbcAliasesAreRecognized(Path path) {
		final var settings = readSettings( path, Map.of(
				JPA_JDBC_URL, "jdbc:legacy", JPA_JDBC_DRIVER, "legacy-driver",
				JPA_JDBC_USER, "legacy-user", JPA_JDBC_PASSWORD, "legacy-pass" ) );
		assertThat( settings ).containsEntry( URL, "jdbc:legacy" )
				.containsEntry( DRIVER, "legacy-driver" ).containsEntry( USER, "legacy-user" )
				.containsEntry( PASS, "legacy-pass" );
		assertThat( settings ).doesNotContainKeys( JAKARTA_JDBC_URL, TRANSACTION_COORDINATOR_STRATEGY );
	}

	@ParameterizedTest
	@EnumSource(value = Path.class, names = { "REGISTRY", "METADATA_SOURCES", "CONFIGURATION" })
	void jakartaJdbcAliasesTakePrecedenceOverLegacyAliases(Path path) {
		final var settings = readSettings( path, Map.of(
				JAKARTA_JDBC_URL, "jdbc:jakarta", JAKARTA_JDBC_DRIVER, "jakarta-driver",
				JAKARTA_JDBC_USER, "jakarta-user", JAKARTA_JDBC_PASSWORD, "jakarta-pass",
				JPA_JDBC_URL, "jdbc:legacy", JPA_JDBC_DRIVER, "legacy-driver",
				JPA_JDBC_USER, "legacy-user", JPA_JDBC_PASSWORD, "legacy-pass" ) );
		assertThat( settings ).containsEntry( URL, "jdbc:jakarta" )
				.containsEntry( DRIVER, "jakarta-driver" ).containsEntry( USER, "jakarta-user" )
				.containsEntry( PASS, "jakarta-pass" );
	}

	@ParameterizedTest
	@EnumSource(value = Path.class, names = { "REGISTRY", "METADATA_SOURCES", "CONFIGURATION" })
	void nativeDatasourceAliasesFollowNamespaceThenTransactionPrecedence(Path path) {
		assertThat( readSettings( path, Map.of( JPA_JTA_DATASOURCE, "legacy-jta" ) ) )
				.containsEntry( DATASOURCE, "legacy-jta" );
		assertThat( readSettings( path, Map.of( JPA_NON_JTA_DATASOURCE, "legacy-non-jta" ) ) )
				.containsEntry( DATASOURCE, "legacy-non-jta" );
		assertThat( readSettings( path, Map.of(
				JPA_NON_JTA_DATASOURCE, "legacy-non-jta", JPA_JTA_DATASOURCE, "legacy-jta" ) ) )
				.containsEntry( DATASOURCE, "legacy-non-jta" );
		assertThat( readSettings( path, Map.of(
				JAKARTA_JTA_DATASOURCE, "jakarta-jta", JPA_NON_JTA_DATASOURCE, "legacy-non-jta",
				JPA_JTA_DATASOURCE, "legacy-jta" ) ) )
				.containsEntry( DATASOURCE, "jakarta-jta" );
		assertThat( readSettings( path, Map.of(
				JAKARTA_NON_JTA_DATASOURCE, "jakarta-non-jta", JPA_NON_JTA_DATASOURCE, "legacy-non-jta",
				JPA_JTA_DATASOURCE, "legacy-jta" ) ) )
				.containsEntry( DATASOURCE, "jakarta-non-jta" );
		assertThat( readSettings( path, Map.of(
				DATASOURCE, "explicit", JAKARTA_NON_JTA_DATASOURCE, "jakarta-non-jta",
				JAKARTA_JTA_DATASOURCE, "jakarta-jta", JPA_NON_JTA_DATASOURCE, "legacy-non-jta",
				JPA_JTA_DATASOURCE, "legacy-jta" ) ) )
				.containsEntry( DATASOURCE, "explicit" );
	}

	@Test
	void registryKeepsNativeDatasourcePrecedenceAndDoesNotDiscardJdbcSettings() {
		final var settings = readSettings( Path.REGISTRY, Map.of(
				JAKARTA_NON_JTA_DATASOURCE, "non-jta", JAKARTA_JTA_DATASOURCE, "jta", JAKARTA_JDBC_URL, "jdbc:url" ) );
		assertThat( settings ).containsEntry( DATASOURCE, "non-jta" ).containsEntry( URL, "jdbc:url" );
		assertThat( readSettings( Path.REGISTRY, Map.of(
				DATASOURCE, "explicit", JAKARTA_NON_JTA_DATASOURCE, "non-jta", JAKARTA_JTA_DATASOURCE, "jta" ) ) )
				.containsEntry( DATASOURCE, "explicit" );
		assertThat( readSettings( Path.REGISTRY, Map.of( JAKARTA_JTA_DATASOURCE, "jta" ) ) )
				.containsEntry( DATASOURCE, "jta" ).doesNotContainKey( TRANSACTION_COORDINATOR_STRATEGY );
	}

	private static Map<String, Object> readSettings(Path path, Map<String, Object> input) {
		final var original = new HashMap<>( input );
		final var settings = new HashMap<>( input );
		settings.put( DIALECT, H2Dialect.class.getName() );
		settings.put( ALLOW_METADATA_ON_BOOT, false );
		settings.put( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() );
		settings.put( HBM2DDL_AUTO, "none" );
		final var originalSettings = new HashMap<>( settings );
		try {
			if ( path.jpa ) {
				final PersistenceConfiguration configuration = path == Path.HIBERNATE_CONFIGURATION
						? new HibernatePersistenceConfiguration( "settings" )
						: new PersistenceConfiguration( "settings" );
				configuration.properties( settings );
				final var builder = configuration instanceof HibernatePersistenceConfiguration hibernateConfiguration
						? new EntityManagerFactoryBuilderImpl( hibernateConfiguration )
						: new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
				try {
					return new HashMap<>( builder.getStandardServiceRegistry().requireService( ConfigurationService.class ).getSettings() );
				}
				finally {
					StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
				}
			}
			if ( path == Path.CONFIGURATION ) {
				final var configuration = new Configuration();
				configuration.getStandardServiceRegistryBuilder().clearSettings();
				configuration.getProperties().clear();
				configuration.getProperties().putAll( settings );
				try ( var factory = configuration.buildSessionFactory() ) {
					return new HashMap<>( factory.unwrap( SessionFactoryImplementor.class ).getServiceRegistry()
							.requireService( ConfigurationService.class ).getSettings() );
				}
			}
			try ( var registry = new StandardServiceRegistryBuilder().clearSettings().applySettings( settings ).build() ) {
				if ( path == Path.METADATA_SOURCES ) {
					try ( var factory = new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
						return new HashMap<>( registry.requireService( ConfigurationService.class ).getSettings() );
					}
				}
				return new HashMap<>( registry.requireService( ConfigurationService.class ).getSettings() );
			}
		}
		finally {
			assertThat( input ).isEqualTo( original );
			assertThat( settings ).isEqualTo( originalSettings );
		}
	}

	enum Path {
		REGISTRY(false), METADATA_SOURCES(false), CONFIGURATION(false), PERSISTENCE_CONFIGURATION(true), HIBERNATE_CONFIGURATION(true);

		final boolean jpa;

		Path(boolean jpa) {
			this.jpa = jpa;
		}
	}
}
