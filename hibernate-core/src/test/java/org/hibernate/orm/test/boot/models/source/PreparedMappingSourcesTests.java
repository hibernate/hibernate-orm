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

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
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
public class PreparedMappingSourcesTests {
	private static final String PACKAGE_NAME = "org.hibernate.orm.test.boot.models.source";
	private static final String MAPPING_FILE = "org/hibernate/orm/test/boot/models/source/available.xml";

	@Test
	void nullCollectionsAreExposedAsEmptyCollections() {
		final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( null, null, null );

		assertThat( resolvedMappingSources.managedClassDetails() ).isEmpty();
		assertThat( resolvedMappingSources.packageDetails() ).isEmpty();
		assertThat( resolvedMappingSources.xmlMappings() ).isEmpty();
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

		final var modelSources = PreparedMappingSources.from(
				new MappingSources(
						List.of(),
						List.of(),
						List.of(),
						List.of( "org/hibernate/orm/test/bootstrap/binding/hbm/BadMapping.xml" ),
						List.of(),
						List.of()
				),
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
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

		final var modelSources = PreparedMappingSources.from(
				new MappingSources(
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of( URI.create( "file:/visible/META-INF/orm.xml" ).toURL() )
				),
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
				),
				mappingSettings
		);

		assertThat( modelSources.xmlMappings() ).isEmpty();
	}

	@Test
	void testMappingSources(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var modelSources = PreparedMappingSources.from(
				new MappingSources(
						List.of( SimpleEntity.class ),
						List.of( MappedEntity.class.getName() ),
						List.of( PACKAGE_NAME ),
						List.of( MAPPING_FILE ),
						List.of(),
						List.of()
				),
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
				),
				SettingsResolver.resolveMappingSettings(
						SettingsResolver.resolveBootstrapSettings( Map.of() ),
						jakarta.persistence.FetchType.LAZY
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

		var modelSources = PreparedMappingSources.from(
				config,
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
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

		var modelSources = PreparedMappingSources.from(
				config,
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
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
		var modelSources = PreparedMappingSources.from(
				puiWrapper,
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
				)
		);

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
		assertThat( modelSources.includeUnlistedStructuralTypes() ).isTrue();
	}

	@Test
	void testPersistenceUnitInfoExcludeUnlistedDisablesStructuralTypeDiscovery(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var pui = new PersistenceUnitInfoAdapter();
		pui.excludeUnlistedClasses = true;
		pui.managedClassNames.add( SimpleEntity.class.getName() );

		var puiWrapper = new PersistenceUnitInfoDescriptor( pui );
		var modelSources = PreparedMappingSources.from(
				puiWrapper,
				new MappingSourcePreparationContext(
						buildingContext.getModelsContext(),
						buildingContext.getServiceRegistry()
				)
		);

		assertThat( modelSources.includeUnlistedStructuralTypes() ).isFalse();
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
