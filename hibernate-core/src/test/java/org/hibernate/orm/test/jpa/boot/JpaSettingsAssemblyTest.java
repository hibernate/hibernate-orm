package org.hibernate.orm.test.jpa.boot;

import java.util.HashMap;
import java.util.Properties;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManagementAction;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.FLUSH_BEFORE_COMPLETION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_VALIDATION_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;

/// Verifies immutable settings assembly and programmatic entry-point precedence.
///
/// @author Steve Ebersole
@BaseUnitTest
class JpaSettingsAssemblyTest {
	@Test
	void integrationOverridesAreCopiedAndAppliedAfterSpecializedResolution() {
		final var configuration = configuration( false )
				.property( URL, "jdbc:unit" ).property( JAKARTA_JDBC_URL, "jdbc:unit" )
				.property( USER, "unit-user" ).property( DATASOURCE, "unit-datasource" )
				.property( "test.remove", "unit" ).property( "test.override", "unit" );
		final var overrides = new HashMap<String, Object>();
		overrides.put( JAKARTA_JDBC_URL, "jdbc:integration" );
		overrides.put( JAKARTA_JDBC_USER, "integration-user" );
		overrides.put( JAKARTA_VALIDATION_MODE, ValidationMode.NONE );
		overrides.put( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE );
		overrides.put( "test.remove", null );
		overrides.put( "test.override", "integration" );
		overrides.put( null, "ignored" );
		final var originalOverrides = new HashMap<>( overrides );
		final var originalProperties = new HashMap<>( configuration.properties() );
		final var descriptor = new PersistenceUnitDescriptorAdapter() {
			@Override public String getName() { return configuration.name(); }
			@Override public Properties getProperties() {
				final var properties = new Properties();
				properties.putAll( configuration.properties() );
				return properties;
			}
		};
		final var resolved = SettingsResolver.resolveBootstrapSettings( descriptor, overrides );

		assertThat( resolved.configurationValues() ).containsEntry( URL, "jdbc:integration" )
				.containsEntry( JAKARTA_JDBC_URL, "jdbc:integration" ).containsEntry( JPA_JDBC_URL, "jdbc:integration" )
				.containsEntry( USER, "integration-user" )
				.containsEntry( JAKARTA_VALIDATION_MODE, ValidationMode.NONE )
				.containsEntry( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
				.containsEntry( "test.override", "integration" ).containsEntry( "test.remove", null ).doesNotContainKey( DATASOURCE );
		assertThat( overrides ).isEqualTo( originalOverrides );
		assertThat( configuration.properties() ).isEqualTo( originalProperties );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void rawSettingsOverrideTypedDefaultsAndSchemaAction(boolean hibernate) {
		final var configuration = configuration( hibernate )
				.transactionType( PersistenceUnitTransactionType.JTA )
				.validationMode( ValidationMode.NONE ).sharedCacheMode( SharedCacheMode.ALL )
				.schemaManagementDatabaseAction( SchemaManagementAction.CREATE )
				.property( JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.RESOURCE_LOCAL )
				.property( JAKARTA_VALIDATION_MODE, ValidationMode.AUTO )
				.property( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
				.property( FLUSH_BEFORE_COMPLETION, true ).property( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" );
		try ( var result = BootstrapPipeline.resolveMetadata( configuration ) ) {
			assertThat( result.configurationValues() )
					.containsEntry( TRANSACTION_COORDINATOR_STRATEGY, "jdbc" )
					.containsEntry( JAKARTA_VALIDATION_MODE, ValidationMode.AUTO )
					.containsEntry( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
					.containsEntry( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" ).containsEntry( FLUSH_BEFORE_COMPLETION, "false" );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void explicitTransactionCoordinatorIsRetained(boolean hibernate) {
		final var coordinator = new JdbcResourceLocalTransactionCoordinatorBuilderImpl();
		try ( var result = BootstrapPipeline.resolveMetadata( configuration( hibernate )
				.property( JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA )
				.property( TRANSACTION_COORDINATOR_STRATEGY, coordinator ) ) ) {
			assertThat( result.configurationValues().get( TRANSACTION_COORDINATOR_STRATEGY ) ).isSameAs( coordinator );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void malformedTransactionTypeRetainsItsFailure(boolean hibernate) {
		assertThatThrownBy( () -> BootstrapPipeline.resolveMetadata(
				configuration( hibernate ).property( JAKARTA_TRANSACTION_TYPE, "bogus" ) ) )
				.isInstanceOf( PersistenceException.class ).hasRootCauseMessage( "Unknown TransactionType: 'bogus'" );
	}

	private static PersistenceConfiguration configuration(boolean hibernateConfiguration) {
		final PersistenceConfiguration configuration = hibernateConfiguration
				? new HibernatePersistenceConfiguration( "assembly" ) : new PersistenceConfiguration( "assembly" );
		return configuration.property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false )
				.property( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() )
				.property( URL, "jdbc:unit" ).property( DRIVER, "unit-driver" );
	}

}
