/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.pipeline.internal.source.AvailableResources;
import org.hibernate.boot.pipeline.internal.source.AvailableResourcesContext;
import org.hibernate.boot.pipeline.internal.source.MappingSourceContributions;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class AvailableResourcesTests {
	private static final String PACKAGE_NAME = "org.hibernate.orm.test.boot.models.source";
	private static final String MAPPING_FILE = "org/hibernate/orm/test/boot/models/source/available.xml";

	@Test
	void nullCollectionsAreExposedAsEmptyCollections() {
		final AvailableResources availableResources = new AvailableResources( null, null, null );

		assertThat( availableResources.managedClassDetails() ).isEmpty();
		assertThat( availableResources.packageDetails() ).isEmpty();
		assertThat( availableResources.xmlMappings() ).isEmpty();
	}

	@Test
	void xmlMappingDisabledIgnoresExplicitMappingResources(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				Map.of( MappingSettings.XML_MAPPING_ENABLED, false )
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				jakarta.persistence.FetchType.LAZY
		);

		final var modelSources = AvailableResources.from(
				new MappingSourceContributions(
						List.of(),
						List.of(),
						List.of(),
						List.of( "org/hibernate/orm/test/bootstrap/binding/hbm/BadMapping.xml" ),
						List.of(),
						List.of()
				),
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				),
				mappingSettings
		);

		assertThat( modelSources.xmlMappings() ).isEmpty();
	}

	@Test
	void xmlMappingDisabledIgnoresDiscoveredMappingUrls(ServiceRegistryScope registryScope) throws Exception {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				Map.of( MappingSettings.XML_MAPPING_ENABLED, false )
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				jakarta.persistence.FetchType.LAZY
		);

		final var modelSources = AvailableResources.from(
				new MappingSourceContributions(
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of( URI.create( "file:/visible/META-INF/orm.xml" ).toURL() )
				),
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				),
				mappingSettings
		);

		assertThat( modelSources.xmlMappings() ).isEmpty();
	}

	@Test
	void testMetadataSourcesSource(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var metadataSources = new MetadataSources( registryScope.getRegistry() );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
		metadataSources.addAnnotatedClassName( MappedEntity.class.getName() );
		metadataSources.addPackage( PACKAGE_NAME );
		metadataSources.addResource( MAPPING_FILE );

		var modelSources = AvailableResources.from(
				metadataSources,
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				)
		);

		assertThat( modelSources.managedClassDetails() ).hasSize( 2 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
	}

	@Test
	@SuppressWarnings("deprecation")
	void testHibernatePersistenceConfigurationScanning(ServiceRegistryScope registryScope) throws Exception {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var config = new HibernatePersistenceConfiguration( "test", new URL( "file:/does-not-need-to-exist/" ) );
		config.property(
				PersistenceSettings.SCANNER,
				new Scanner() {
					@Override
					public ScanningResult scan(URL... urls) {
						return new ScanningResult() {
							@Override
							public Set<String> discoveredPackages() {
								return Set.of( PACKAGE_NAME );
							}

							@Override
							public Set<String> discoveredClasses() {
								return Set.of( MappedEntity.class.getName() );
							}

							@Override
							public Set<java.net.URI> mappingFiles() {
								return Set.of();
							}
						};
					}

					@Override
					public ScanningResult jpaScan(
							ArchiveDescriptor archiveDescriptor,
							JaxbPersistenceImpl.JaxbPersistenceUnitImpl persistenceUnit) {
						return ScanningResult.NONE;
					}
				}
		);

		var modelSources = AvailableResources.from(
				config,
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				)
		);

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
	}

	@Test
	void testPersistenceConfigurationSource(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		var classLoading = registryScope.getRegistry().requireService( ClassLoaderService.class );

		var config = new HibernatePersistenceConfiguration( "test" );
		config.managedClass( SimpleEntity.class );
		config.managedClass( classLoading.classForName( PACKAGE_NAME + ".package-info" ) );
		config.mappingFile( MAPPING_FILE );

		var modelSources = AvailableResources.from(
				config,
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				)
		);

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
	}

	@Test
	void testPersistenceUnitInfoSource(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var pui = new PersistenceUnitInfoAdapter();
		pui.managedClassNames.add( SimpleEntity.class.getName() );
		pui.managedClassNames.add( PACKAGE_NAME + ".package-info" );
		pui.mappingFiles.add( MAPPING_FILE );

		var puiWrapper = new PersistenceUnitInfoDescriptor( pui );
		var modelSources = AvailableResources.from(
				puiWrapper,
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				)
		);

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
		assertThat( modelSources.includeUnlistedPersistentSuperclasses() ).isTrue();
	}

	@Test
	void testPersistenceUnitInfoExcludeUnlistedDisablesPersistentSuperclassCompletion(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var pui = new PersistenceUnitInfoAdapter();
		pui.excludeUnlistedClasses = true;
		pui.managedClassNames.add( SimpleEntity.class.getName() );

		var puiWrapper = new PersistenceUnitInfoDescriptor( pui );
		var modelSources = AvailableResources.from(
				puiWrapper,
				new AvailableResourcesContext(
						buildingContext.getBootstrapContext().getModelsContext(),
						buildingContext.getBootstrapContext().getServiceRegistry()
				)
		);

		assertThat( modelSources.includeUnlistedPersistentSuperclasses() ).isFalse();
	}

	private static class PersistenceUnitInfoAdapter extends org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter {
		private final List<String> managedClassNames = new ArrayList<>();
		private final List<String> mappingFiles = new ArrayList<>();
		private boolean excludeUnlistedClasses;

		@Override
		public List<String> getMappingFileNames() {
			return mappingFiles;
		}

		@Override
		public List<String> getManagedClassNames() {
			return managedClassNames;
		}

		@Override
		public boolean excludeUnlistedClasses() {
			return excludeUnlistedClasses;
		}
	}
}
