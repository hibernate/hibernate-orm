/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.MappingSettingsResolver;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;

/**
 * @author Steve Ebersole
 */
public class BootstrapSettingsResolverTests {
	@Test
	void simpleMapResolutionUsesNativeDefaults() {
		final var settings = BootstrapSettingsResolver.resolve( Map.of() );

		assertThat( settings.configurationValues() )
				.containsEntry( "java.version", Environment.getProperties().get( "java.version" ) );
		assertThat( settings.jpaBootstrap() ).isFalse();

		final var mappingSettings = MappingSettingsResolver.resolve( settings, FetchType.EAGER );
		assertThat( mappingSettings.xmlMappingEnabled() ).isTrue();
		assertThat( mappingSettings.validateXml() ).isFalse();
		assertThat( mappingSettings.defaultToOneFetchType() ).isEqualTo( FetchType.EAGER );
		assertThat( mappingSettings.createImplicitDiscriminatorsForJoinedInheritance() ).isFalse();
		assertThat( mappingSettings.ignoreExplicitDiscriminatorsForJoinedInheritance() ).isFalse();
		assertThat( mappingSettings.cacheRegionDefinitions() ).isEmpty();
	}

	@Test
	void simpleMapResolutionHonorsExplicitValues() {
		final var settings = BootstrapSettingsResolver.resolve( Map.of(
				MappingSettings.XML_MAPPING_ENABLED, "false",
				MappingSettings.VALIDATE_XML, true,
				MappingSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, true,
				MappingSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, "true",
				CLASS_CACHE_PREFIX + ".org.acme.TheEntity", "read-write;entities;all",
				COLLECTION_CACHE_PREFIX + ".org.acme.TheEntity.items", "nonstrict-read-write,items"
		) );

		final var mappingSettings = MappingSettingsResolver.resolve( settings, FetchType.EAGER );
		assertThat( mappingSettings.xmlMappingEnabled() ).isFalse();
		assertThat( mappingSettings.validateXml() ).isTrue();
		assertThat( mappingSettings.createImplicitDiscriminatorsForJoinedInheritance() ).isTrue();
		assertThat( mappingSettings.ignoreExplicitDiscriminatorsForJoinedInheritance() ).isTrue();
		assertThat( mappingSettings.cacheRegionDefinitions() )
				.extracting( "regionType", "role", "usage", "region", "cacheLazy" )
				.containsExactlyInAnyOrder(
						tuple( CacheRegionType.ENTITY, "org.acme.TheEntity", "read-write", "entities", true ),
						tuple( CacheRegionType.COLLECTION, "org.acme.TheEntity.items", "nonstrict-read-write", "items", false )
				);
	}

	@Test
	void environmentPropertiesAreUsedAsBaselineAndExplicitValuesOverride() {
		final var settings = BootstrapSettingsResolver.resolve(
				Map.of( "java.version", "explicit" )
		);

		assertThat( settings.configurationValues() )
				.containsEntry( "java.version", "explicit" );
	}

	@Test
	void persistenceUnitResolutionOverlaysIntegrationSettings() {
		final var persistenceUnitProperties = new Properties();
		persistenceUnitProperties.put( MappingSettings.XML_MAPPING_ENABLED, true );

		final var settings = BootstrapSettingsResolver.resolve(
				new TestPersistenceUnitDescriptor( persistenceUnitProperties ),
				Map.of( MappingSettings.XML_MAPPING_ENABLED, false )
		);

		assertThat( settings.jpaBootstrap() ).isTrue();
		final var mappingSettings = MappingSettingsResolver.resolve( settings, FetchType.LAZY );
		assertThat( mappingSettings.xmlMappingEnabled() ).isFalse();
		assertThat( mappingSettings.defaultToOneFetchType() ).isEqualTo( FetchType.LAZY );
	}

	private record TestPersistenceUnitDescriptor(Properties properties) implements PersistenceUnitDescriptor {
		@Override
		public String getName() {
			return "test-unit";
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		@Override
		public String getProviderClassName() {
			return null;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			return true;
		}

		@Override
		public FetchType getDefaultToOneFetchType() {
			return FetchType.LAZY;
		}

		@Override
		public boolean isUseQuotedIdentifiers() {
			return false;
		}

		@Override
		public List<String> getManagedClassNames() {
			return List.of();
		}

		@Override
		public List<String> getAllClassNames() {
			return List.of();
		}

		@Override
		public List<String> getMappingFileNames() {
			return List.of();
		}

		@Override
		public List<URL> getJarFileUrls() {
			return List.of();
		}

		@Override
		public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}

		@Override
		public Object getNonJtaDataSource() {
			return null;
		}

		@Override
		public Object getJtaDataSource() {
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			return ValidationMode.NONE;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return SharedCacheMode.UNSPECIFIED;
		}

		@Override
		public Properties getProperties() {
			return properties;
		}

		@Override
		public ClassLoader getClassLoader() {
			return null;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public boolean isClassTransformerRegistrationDisabled() {
			return true;
		}

		@Override
		public org.hibernate.bytecode.spi.ClassTransformer pushClassTransformer(
				org.hibernate.bytecode.enhance.spi.EnhancementContext enhancementContext) {
			return null;
		}
	}
}
