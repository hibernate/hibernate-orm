/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.ContributionDiscoveryContext;
import org.hibernate.boot.pipeline.internal.MappingCustomizations;
import org.hibernate.boot.pipeline.internal.MappingResolutionPipeline;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.internal.ScanningResultImpl;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class MappingSourcesTests {
	private static final String PACKAGE_NAME = "org.hibernate.orm.test.boot.models.source";

	@Test
	void adaptsMappingSources() throws Exception {
		final Path mappingPath = Path.of( "src/test/resources/mappings/complete/simple-complete.xml" );
		final File mappingFile = new File( "/tmp/simple-complete-file.xml" );
		final URI mappingUri = URI.create( "file:/tmp/simple-complete.xml" );
		final URL mappingUrl = URI.create( "file:/tmp/simple-complete-url.xml" ).toURL();

		final var mappingSources = MappingSources.from(
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

		assertThat( mappingSources.managedClasses() ).containsExactly( SimpleEntity.class );
		assertThat( mappingSources.managedClassNames() ).containsExactly( "example.Managed" );
		assertThat( mappingSources.packageNames() ).containsExactly( "example.package" );
		assertThat( mappingSources.mappingResources() ).containsExactly( "mappings/complete/simple-complete.xml" );
		assertThat( mappingSources.mappingFileUris() ).containsExactly(
				mappingPath.toUri(),
				mappingFile.toPath().toUri(),
				mappingUri
		);
		assertThat( mappingSources.mappingFileUrls() ).containsExactly( mappingUrl );
	}

	@Test
	void appliesMetadataCustomizationQueryImports(ServiceRegistryScope registryScope) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( Map.of() );
		final var mappingSettings = SettingsResolver.resolveMappingSettings( bootstrapSettings, FetchType.EAGER );
		final var mappingSources = MappingSources.from(
				new MappingSources()
						.addManagedClass( SimpleEntity.class )
		);
		final var mappingCustomizations = new MappingCustomizations(
				Map.of( "Simple", SimpleEntity.class ),
				null,
				null,
				null
		);

		final var resolvedMapping = MappingResolutionPipeline.resolve(
				bootstrapSettings,
				mappingSettings,
				mappingSources,
				mappingCustomizations,
				registryScope.getRegistry()
		);

		assertThat( resolvedMapping.metadata().getImports() )
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

		final var mappingSources = MappingSources.from(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				new ContributionDiscoveryContext( registryScope.getRegistry().requireService( ClassLoaderService.class ) )
		);

		assertThat( mappingSources.managedClasses() ).containsExactly( SimpleEntity.class );
		assertThat( mappingSources.managedClassNames() ).isEmpty();
		assertThat( mappingSources.packageNames() ).isEmpty();
		assertThat( mappingSources.mappingResources() ).containsExactly( "mappings/complete/simple-complete.xml" );
		assertThat( mappingSources.mappingFileUris() ).isEmpty();
	}

	@Test
	void adaptsPersistenceUnitDescriptorSources() {
		final var persistenceUnitDescriptor = new TestPersistenceUnitDescriptor(
				new Properties(),
				List.of( MappedEntity.class.getName(), PACKAGE_NAME + ".package-info" ),
				List.of( "org/hibernate/orm/test/boot/models/source/available.xml" )
		);
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				persistenceUnitDescriptor,
				Map.of()
		);
		final var mappingSources = MappingSources.from(
				persistenceUnitDescriptor,
				bootstrapSettings,
				null
		);

		assertThat( mappingSources.managedClasses() ).isEmpty();
		assertThat( mappingSources.managedClassNames() ).containsExactly( MappedEntity.class.getName() );
		assertThat( mappingSources.packageNames() ).containsExactly( PACKAGE_NAME );
		assertThat( mappingSources.mappingResources() ).containsExactly(
				"org/hibernate/orm/test/boot/models/source/available.xml"
		);
		assertThat( mappingSources.includeUnlistedStructuralTypes() ).isFalse();
	}

	@Test
	void adaptsPersistenceUnitDescriptorJarFileScanning(ServiceRegistryScope registryScope) throws Exception {
		final var scanner = new CapturingScanner();
		final var persistenceUnitProperties = new Properties();
		persistenceUnitProperties.put( PersistenceSettings.SCANNER, scanner );
		final var rootUrl = URI.create( "file:/persistence-root/" ).toURL();
		final var jarFileUrl = URI.create( "file:/persistence-root/lib/model.jar" ).toURL();
		final var descriptor = new TestPersistenceUnitDescriptor(
				persistenceUnitProperties,
				List.of(),
				List.of(),
				rootUrl,
				List.of( jarFileUrl ),
				true
		);
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				descriptor,
				Map.of()
		);

		final var mappingSources = MappingSources.from(
				descriptor,
				bootstrapSettings,
				new ContributionDiscoveryContext( registryScope.getRegistry().requireService( ClassLoaderService.class ) )
		);

		assertThat( scanner.boundaries ).containsExactly( jarFileUrl );
		assertThat( mappingSources.managedClassNames() ).containsExactly( "example.ScannedEntity" );
		assertThat( mappingSources.packageNames() ).containsExactly( "example.packageinfo" );
		assertThat( mappingSources.mappingFileUris() )
				.containsExactly( URI.create( "file:/persistence-root/lib/META-INF/orm.xml" ) );
		assertThat( mappingSources.includeUnlistedStructuralTypes() ).isFalse();
	}

	private record TestPersistenceUnitDescriptor(
			Properties properties,
			List<String> classNames,
			List<String> mappingFileNames,
			URL rootUrl,
			List<URL> jarFileUrls,
			boolean excludeUnlistedClasses) implements PersistenceUnitDescriptor {
		private TestPersistenceUnitDescriptor(
				Properties properties,
				List<String> classNames,
				List<String> mappingFileNames) {
			this( properties, classNames, mappingFileNames, null, List.of(), true );
		}

		@Override
		public String getName() {
			return "test-unit";
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return rootUrl;
		}

		@Override
		public String getProviderClassName() {
			return null;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			return excludeUnlistedClasses;
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
			return classNames;
		}

		@Override
		public List<String> getAllClassNames() {
			return classNames;
		}

		@Override
		public List<String> getMappingFileNames() {
			return mappingFileNames;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return jarFileUrls;
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

	private static class CapturingScanner implements Scanner {
		private List<URL> boundaries = List.of();

		@Override
		public ScanningResult scan(URL... boundaries) {
			this.boundaries = Arrays.asList( boundaries );
			return new ScanningResultImpl(
					Set.of( "example.packageinfo" ),
					Set.of( "example.ScannedEntity" ),
					Set.of( URI.create( "file:/persistence-root/lib/META-INF/orm.xml" ) )
			);
		}

		@Override
		public ScanningResult jpaScan(
				org.hibernate.boot.archive.spi.ArchiveDescriptor archiveDescriptor,
				org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit) {
			return ScanningResult.NONE;
		}
	}
}
