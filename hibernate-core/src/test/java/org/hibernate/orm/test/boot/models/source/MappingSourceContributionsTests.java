/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.hibernate.boot.internal.MappingSources;
import org.hibernate.boot.pipeline.internal.source.MappingSourceContributions;
import org.hibernate.boot.pipeline.internal.MetadataCustomizations;
import org.hibernate.boot.pipeline.internal.MetadataResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class MappingSourceContributionsTests {
	@Test
	void adaptsMappingSources() throws Exception {
		final Path mappingPath = Path.of( "src/test/resources/mappings/complete/simple-complete.xml" );
		final File mappingFile = new File( "/tmp/simple-complete-file.xml" );
		final URI mappingUri = URI.create( "file:/tmp/simple-complete.xml" );
		final URL mappingUrl = URI.create( "file:/tmp/simple-complete-url.xml" ).toURL();

		final var sourceContributions = MappingSourceContributions.from(
				new MappingSources()
						.addManagedClass( SimpleEntity.class )
						.addManagedClassName( "example.Managed" )
						.addPackage( "example.package" )
						.addMappingResource( "mappings/complete/simple-complete.xml" )
						.addMappingFile( mappingPath )
						.addMappingFile( mappingFile )
						.addMappingUri( mappingUri )
						.addMappingUrl( mappingUrl )
		);

		assertThat( sourceContributions.managedClasses() ).containsExactly( SimpleEntity.class );
		assertThat( sourceContributions.managedClassNames() ).containsExactly( "example.Managed" );
		assertThat( sourceContributions.packageNames() ).containsExactly( "example.package" );
		assertThat( sourceContributions.mappingResources() ).containsExactly( "mappings/complete/simple-complete.xml" );
		assertThat( sourceContributions.mappingFileUris() ).containsExactly(
				mappingPath.toUri(),
				mappingFile.toPath().toUri(),
				mappingUri
		);
		assertThat( sourceContributions.mappingFileUrls() ).containsExactly( mappingUrl );
	}

	@Test
	void appliesMetadataCustomizationQueryImports(ServiceRegistryScope registryScope) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( Map.of() );
		final var mappingSettings = SettingsResolver.resolveMappingSettings( bootstrapSettings, FetchType.EAGER );
		final var sourceContributions = MappingSourceContributions.from(
				new MappingSources()
						.addManagedClass( SimpleEntity.class )
		);
		final var metadataCustomizations = new MetadataCustomizations(
				Map.of( "Simple", SimpleEntity.class ),
				null,
				null,
				null
		);

		final var resolvedMetadata = MetadataResolver.resolve(
				bootstrapSettings,
				mappingSettings,
				sourceContributions,
				metadataCustomizations,
				registryScope.getRegistry()
		);

		assertThat( resolvedMetadata.metadata().getImports() )
				.containsEntry( "Simple", SimpleEntity.class.getName() );
	}

	@Test
	void adaptsHibernatePersistenceConfigurationSources(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.managedClass( SimpleEntity.class );
		persistenceConfiguration.mappingFile( "mappings/complete/simple-complete.xml" );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				persistenceConfiguration,
				Map.of()
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				persistenceConfiguration.defaultToOneFetchType()
		);

		final var sourceContributions = MappingSourceContributions.from(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				registryScope.getRegistry().requireService( ClassLoaderService.class )
		);

		assertThat( sourceContributions.managedClasses() ).containsExactly( SimpleEntity.class );
		assertThat( sourceContributions.managedClassNames() ).isEmpty();
		assertThat( sourceContributions.packageNames() ).isEmpty();
		assertThat( sourceContributions.mappingResources() ).containsExactly( "mappings/complete/simple-complete.xml" );
		assertThat( sourceContributions.mappingFileUris() ).isEmpty();
	}
}
