/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

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
import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
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

/// Characterizes JPA source precedence independently of registry alias normalization.
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
		final var builder = new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), overrides );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( URL, "jdbc:integration" )
					.containsEntry( JAKARTA_JDBC_URL, "jdbc:integration" ).containsEntry( JPA_JDBC_URL, "jdbc:integration" )
					.containsEntry( USER, "integration-user" )
					.containsEntry( JAKARTA_VALIDATION_MODE, ValidationMode.NONE )
					.containsEntry( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
					.containsEntry( "test.override", "integration" ).doesNotContainKeys( "test.remove", DATASOURCE );
			assertThat( overrides ).isEqualTo( originalOverrides );
			assertThat( configuration.properties() ).isEqualTo( originalProperties );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void retainsEntrySpecificTypedValuesAndSchemaActionPrecedence(boolean hibernateConfiguration) {
		final var configuration = configuration( hibernateConfiguration )
				.transactionType( PersistenceUnitTransactionType.JTA )
				.validationMode( ValidationMode.NONE ).sharedCacheMode( SharedCacheMode.ALL )
				.schemaManagementDatabaseAction( SchemaManagementAction.CREATE )
				.property( JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.RESOURCE_LOCAL )
				.property( JAKARTA_VALIDATION_MODE, ValidationMode.AUTO )
				.property( JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
				.property( FLUSH_BEFORE_COMPLETION, true )
				.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" );
		final var builder = builder( configuration );
		try {
			assertThat( builder.getConfigurationValues() )
					.containsEntry( TRANSACTION_COORDINATOR_STRATEGY, hibernateConfiguration
							? JdbcResourceLocalTransactionCoordinatorBuilderImpl.class : JtaTransactionCoordinatorBuilderImpl.class )
					.containsEntry( JAKARTA_VALIDATION_MODE, hibernateConfiguration ? ValidationMode.AUTO : ValidationMode.NONE )
					.containsEntry( JAKARTA_SHARED_CACHE_MODE, hibernateConfiguration ? SharedCacheMode.NONE : SharedCacheMode.ALL )
					.containsEntry( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" );
			assertThat( builder.getConfigurationValues() ).containsEntry( FLUSH_BEFORE_COMPLETION, "false" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	@Test
	void baselineCustomizerPrecedesUnitPropertiesAndIntegrationOverrides() {
		final var configuration = configuration( false ).property( "test.unit", "unit" );
		final var builder = new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration ), Map.of( "test.integration", "integration" ),
				baseline -> {
					baseline.getConfigurationValues().put( "test.unit", "baseline" );
					baseline.getConfigurationValues().put( "test.integration", "baseline" );
					baseline.getConfigurationValues().put( "test.baseline", "baseline" );
				} );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( "test.unit", "unit" )
					.containsEntry( "test.integration", "integration" ).containsEntry( "test.baseline", "baseline" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void explicitTransactionCoordinatorIsRetained(boolean hibernateConfiguration) {
		final var coordinator = new JdbcResourceLocalTransactionCoordinatorBuilderImpl();
		final var builder = builder( configuration( hibernateConfiguration )
				.property( JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA )
				.property( TRANSACTION_COORDINATOR_STRATEGY, coordinator ) );
		try {
			assertThat( builder.getConfigurationValues().get( TRANSACTION_COORDINATOR_STRATEGY ) ).isSameAs( coordinator );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void malformedTransactionTypeRetainsItsFailure(boolean hibernateConfiguration) {
		assertThatThrownBy( () -> {
			if ( hibernateConfiguration ) {
				builder( configuration( true ).property( JAKARTA_TRANSACTION_TYPE, "bogus" ) );
			}
			else {
				new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration( false ) ),
						Map.of( JAKARTA_TRANSACTION_TYPE, "bogus" ) );
			}
		} )
				.isInstanceOf( PersistenceException.class ).hasMessage( "Unknown TransactionType: 'bogus'" );
	}

	@Test
	void malformedCacheDefinitionRetainsPersistenceUnitDiagnostic() {
		assertThatThrownBy( () -> builder( configuration( false ).property( CLASS_CACHE_PREFIX + ".Entity", "" ) ) )
				.isInstanceOf( PersistenceException.class ).hasMessageContaining( "Cache region configuration" )
				.hasMessageContaining( "[persistence unit: assembly]" );
	}

	private static PersistenceConfiguration configuration(boolean hibernateConfiguration) {
		final PersistenceConfiguration configuration = hibernateConfiguration
				? new HibernatePersistenceConfiguration( "assembly" ) : new PersistenceConfiguration( "assembly" );
		return configuration.property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false )
				.property( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() )
				.property( URL, "jdbc:unit" ).property( DRIVER, "unit-driver" );
	}

	private static EntityManagerFactoryBuilderImpl builder(PersistenceConfiguration configuration) {
		return configuration instanceof HibernatePersistenceConfiguration hibernateConfiguration
				? new EntityManagerFactoryBuilderImpl( hibernateConfiguration )
				: new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
	}
}
