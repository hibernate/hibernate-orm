/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.models.UnknownClassException;
import org.hibernate.annotations.FilterDef;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.ClassTransformer;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.MappingException;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.ContributionDiscoveryContext;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.process.internal.EnhancementCandidates;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.scan.internal.ScanningResultImpl;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.models.dynamic.DynamicClassDetails;
import org.hibernate.models.jdk.JdkClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.testing.orm.module.TestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Regression coverage for the resource inventory and resolved category contracts.
///
/// @author Steve Ebersole
class ManagedResourcesTests {
	static class SampleType {
	}

	@Test
	void programmaticBootstrapScansOnceAndSkipsDisabledXml(@TempDir Path directory) throws Exception {
		final var calls = new AtomicInteger();
		final var root = directory.toUri().toURL();
		for ( boolean withBoundary : new boolean[] { false, true } ) {
			final var configuration = new HibernatePersistenceConfiguration( "inventory", withBoundary ? root : null );
			configuration.property( MappingSettings.XML_MAPPING_ENABLED, false );
			configuration.property( PersistenceSettings.SCANNER, new Scanner() {
				@Override
				public ScanningResult scan(URL... boundaries) {
					calls.incrementAndGet();
					assertThat( boundaries ).containsExactly( root );
					return new ScanningResultImpl( Set.of( "java.base" ), Set.of( "example" ), Set.of( SampleType.class.getName() ),
							Set.of( directory.resolve( "does-not-exist.xml" ).toUri() ) );
				}

				@Override
				public ScanningResult jpaScan(ArchiveDescriptor archive,
						JaxbPersistenceImpl.JaxbPersistenceUnitImpl unit) {
					throw new AssertionError( "Unexpected persistence.xml scan" );
				}
			} );

			try ( var registry = new StandardServiceRegistryBuilder().build() ) {
				final var settings = SettingsResolver.resolveBootstrapSettings( configuration );
				final var mappingSettings = SettingsResolver.resolveMappingSettings( settings, configuration.defaultToOneFetchType() );
				final var sources = MappingSources.from( configuration, settings, mappingSettings,
						new ContributionDiscoveryContext( registry.requireService( ClassLoaderService.class ) ) );
				assertThat( calls.get() ).isEqualTo( withBoundary ? 1 : 0 );
				if ( withBoundary ) {
					assertThat( sources.managedClassNames() ).containsExactly( SampleType.class.getName() );
					assertThat( sources.packageNames() ).containsExactly( "example" );
					assertThat( sources.moduleNames() ).containsExactly( "java.base" );
				}
				// XML remains lazy during collection and is skipped during preparation.
				final var xmlOnly = new MappingSources().addMappingUris( sources.mappingFileUris() );
				final var context = new MetadataBuildingContextTestingImpl( registry );
				assertThat( PreparedMappingSources.from( xmlOnly,
						new MappingSourcePreparationContext( context.getModelsContext(), registry ), mappingSettings ).xmlMappings() ).isEmpty();
			}
		}
	}

	@Test
	void descriptorOnlyMetadataContributesWithoutEnrollingPackageClasses() {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var metadata = MetadataBuildingHelper.buildMetadata( registry, new MappingSources()
					.addPackageDescriptor( "org.hibernate.orm.test.boot.models.inventory" ) );
			assertThat( metadata.getEntityBindings() ).isEmpty();
			assertThat( metadata.getFilterDefinitions() ).containsKey( "inventoryFilter" );
		}
	}

	@Test
	void conflictingLoadedClassesFailWithoutChoosingAClassLoader() throws Exception {
		final byte[] bytes;
		try ( var stream = SampleType.class.getResourceAsStream( "ManagedResourcesTests$SampleType.class" ) ) {
			bytes = stream.readAllBytes();
		}
		final var alternate = new ClassLoader( getClass().getClassLoader() ) {
			Class<?> define() {
				return defineClass( SampleType.class.getName(), bytes, 0, bytes.length );
			}
		}.define();
		assertThatThrownBy( () -> new MappingSources().addManagedClass( SampleType.class ).addManagedClass( alternate ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( SampleType.class.getName() );
	}

	@Test
	void bothTransformerPathsUseCompleteContainerInventory(@TempDir Path directory) throws Exception {
		final var root = directory.toUri().toURL();
		for ( boolean direct : new boolean[] { true, false } ) {
			final var reads = new LinkedHashSet<String>();
			final var loader = new ClassLoader( "inventory-" + UUID.randomUUID(), getClass().getClassLoader() ) {
				@Override
				public URL getResource(String name) {
					reads.add( name );
					return super.getResource( name );
				}

				@Override
				public InputStream getResourceAsStream(String name) {
					reads.add( name );
					return super.getResourceAsStream( name );
				}
			};
			final var registered = new ArrayList<ClassTransformer>();
			final var entityName = "org.hibernate.orm.test.boot.models.inventory.UnlistedEntity";
			final var unit = new PersistenceUnitInfoAdapter() {
				@Override
				public URL getPersistenceUnitRootUrl() { return root; }
				@Override
				public ClassLoader getClassLoader() { return loader; }
				@Override
				public ClassLoader getNewTempClassLoader() { return loader; }
				@Override
				public List<String> getAllClassNames() { return List.of( entityName, entityName ); }
				@Override
				public List<String> getAllPackageDescriptors() { return List.of( "org.hibernate.orm.test.boot.models.inventory" ); }
				@Override
				public List<String> getAllModuleDescriptors() { return List.of( "java.base" ); }
				@Override
				public void addTransformer(ClassTransformer transformer) { registered.add( transformer ); }
			};
			final var settings = new java.util.HashMap<>( ServiceRegistryUtil.createBaseSettings() );
			settings.put( PersistenceSettings.SCANNER, new Scanner() {
				@Override
				public ScanningResult scan(URL... boundaries) {
					throw new AssertionError( "Container inventory must not trigger scanning" );
				}

				@Override
				public ScanningResult jpaScan(ArchiveDescriptor archive, JaxbPersistenceImpl.JaxbPersistenceUnitImpl unit) {
					throw new AssertionError( "Container inventory must not trigger scanning" );
				}
			} );
			final var provider = new HibernatePersistenceProvider();
			if ( direct ) {
				assertThat( provider.getClassTransformer( unit, Map.of() ) ).isNotNull();
				assertThat( provider.getClassTransformer( unit, Map.of() ) ).isNull();
			}
			else {
				try ( var factory = provider.createContainerEntityManagerFactory( unit, settings ) ) {
					assertThat( registered ).hasSize( 1 );
				}
			}
			assertThat( reads ).contains( entityName.replace( '.', '/' ) + ".class" );
			if ( direct ) {
				assertThat( reads ).noneMatch( name -> name.endsWith( "package-info.class" ) || name.endsWith( "module-info.class" ) );
			}
		}
	}

	@Test
	void disabledXmlIsNotProcessedFromAnExternalBatch() {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var context = new MetadataBuildingContextTestingImpl( registry );
			final var settings = SettingsResolver.resolveMappingSettings(
					SettingsResolver.resolveBootstrapSettings( Map.of( MappingSettings.XML_MAPPING_ENABLED, false ) ),
					jakarta.persistence.FetchType.EAGER );
			final var source = PreparedMappingSources.from( new MappingSources().addMappingResource( "missing.xml" ),
					new MappingSourcePreparationContext( context.getModelsContext(), registry ), settings );
			assertThat( source.managedClassDetails() ).isEmpty();
			assertThat( source.xmlMappings() ).isEmpty();
		}
	}


	@Test
	void sourceCopiesAndPreparedSnapshotsRetainIdentity() {
		final var sources = new MappingSources().addManagedClass( String.class ).addManagedClass( String.class )
				.addManagedClassName( "example.NotLoaded" ).addPackageDescriptor( "example" )
				.addModuleDescriptor( "example.module" ).addMappingResource( "mapping.xml" ).addMappingResource( "mapping.xml" );
		final var copy = MappingSources.from( sources );
		sources.addManagedClass( Integer.class ).addPackageDescriptor( "another" );
		assertThat( copy.managedClasses() ).containsExactly( String.class );
		assertThat( copy.managedClassNames() ).containsExactly( "example.NotLoaded" );
		assertThat( copy.packageNames() ).containsExactly( "example" );
		assertThat( copy.moduleNames() ).containsExactly( "example.module" );
		assertThat( copy.mappingResources() ).containsExactly( "mapping.xml", "mapping.xml" );
		assertThatThrownBy( () -> copy.managedClassNames().clear() ).isInstanceOf( UnsupportedOperationException.class );

		final var context = SourceModelTestHelper.createBuildingContext( SampleType.class );
		final var details = context.getClassDetailsRegistry().resolveClassDetails( SampleType.class.getName() );
		final var classes = new ArrayList<>( List.of( details ) );
		final var xml = new Binding<>( new JaxbEntityMappingsImpl(), new Origin( SourceType.OTHER, "test" ) );
		final var mappings = new ArrayList<>( List.of( xml, xml ) );
		final var prepared = new PreparedMappingSources( classes, List.of(), mappings );
		classes.clear();
		mappings.clear();
		assertThat( prepared.managedClassDetails() ).containsExactly( details );
		assertThat( prepared.xmlMappings() ).containsExactly( xml, xml );
		assertThatThrownBy( () -> prepared.managedClassDetails().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> prepared.xmlMappings().clear() ).isInstanceOf( UnsupportedOperationException.class );
	}


	@Test
	void modelsClassLoadingDistinguishesMissingClassesFromBrokenClasses() {
		final var service = mock( ClassLoaderService.class );
		final var missing = new ClassLoadingException(
				"missing", new ClassNotFoundException( "missing" ) );
		when( service.classForName( "missing" ) ).thenThrow( missing );
		final var aggregatedFailure = new ClassNotFoundException( "broken" );
		aggregatedFailure.addSuppressed( new ClassNotFoundException( "broken" ) );
		aggregatedFailure.addSuppressed( new UnsupportedClassVersionError( "broken" ) );
		final var broken = new ClassLoadingException( "broken", aggregatedFailure );
		when( service.classForName( "broken" ) ).thenThrow( broken );
		final var loading = new ClassLoaderServiceLoading( service );
		assertThatThrownBy( () -> loading.classForName( "missing" ) )
				.isInstanceOf( UnknownClassException.class ).hasCause( missing );
		assertThatThrownBy( () -> loading.classForName( "broken" ) ).isSameAs( broken );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void packageResolutionKeepsCategoriesAndMissingPlaceholdersSeparate(boolean jandex) throws Exception {
		final var packageName = "org.hibernate.orm.test.boot.models.inventory";
		final var packageInfo = Class.forName( packageName + ".package-info" );
		final var context = SourceModelTestHelper.createBuildingContext(
				jandex ? SourceModelTestHelper.buildJandexIndex( packageInfo, SampleType.class ) : null,
				SampleType.class );
		final var types = context.getClassDetailsRegistry();
		final var packageDetails = types.resolvePackageDetails( packageName );
		assertThat( packageDetails.isRealClass() ).isTrue();
		assertThat( packageDetails.getDirectAnnotationUsage( FilterDef.class ) ).isNotNull();
		assertThat( types.resolvePackageDetails( packageName ) ).isSameAs( packageDetails );
		assertThat( types.findPackageDetails( packageName ) ).isSameAs( packageDetails );
		assertThat( types.getPackageDetails( packageName ) ).isSameAs( packageDetails );

		final var missing = types.resolvePackageDetails( "does.not.exist" );
		assertThat( missing.isRealClass() ).isFalse();
		assertThat( types.resolvePackageDetails( "does.not.exist" ) ).isSameAs( missing );
		assertThat( types.findPackageDetails( "does.not.exist" ) ).isSameAs( missing );
		final var ordinary = types.resolveClassDetails( SampleType.class.getName() );
		final var ambiguousPackage = types.resolvePackageDetails( SampleType.class.getName() );
		assertThat( ambiguousPackage.isRealClass() ).isFalse();
		assertThat( ambiguousPackage ).isNotSameAs( ordinary );
		assertThat( types.resolveClassDetails( SampleType.class.getName() ) ).isSameAs( ordinary );
		assertThat( org.hibernate.boot.model.internal.GeneratorAnnotationHelper.locatePackageInfoDetails( ordinary, types ) ).isNull();

		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var preparation = new MappingSourcePreparationContext( context, registry );
			final var settings = SettingsResolver.resolveMappingSettings(
					SettingsResolver.resolveBootstrapSettings( Map.of() ), jakarta.persistence.FetchType.EAGER );
			final var source = PreparedMappingSources.from( new MappingSources().addManagedClassName( SampleType.class.getName() )
					.addPackageDescriptor( packageName ), preparation, settings );
			assertThat( source.packageDetails() ).containsExactly( packageDetails );
			assertThat( source.managedClassDetails() ).containsExactly( ordinary );
			for ( var missingPackage : new String[] { "does.not.exist", SampleType.class.getPackageName(), SampleType.class.getName() } ) {
				assertThatThrownBy( () -> PreparedMappingSources.from( new MappingSources().addPackageDescriptor( missingPackage ),
						preparation, settings ) ).isInstanceOf( UnknownClassException.class )
						.hasMessageContaining( missingPackage + ".package-info" );
			}
		}
	}

	@Test
	void nativeDescriptorsAndAliasesAgree() {
		final var sources = new MappingSources();
		assertThat( sources.addPackageDescriptor( "example." ) ).isSameAs( sources );
		sources.addPackage( "example" ).addPackageDescriptor( "example" );
		sources.addPackage( String.class.getPackage() );
		sources.addModule( String.class.getModule() );
		assertThat( sources.packageNames() ).containsExactly( "example", "java.lang" );
		assertThat( sources.moduleNames() ).containsExactly( "java.base" );
		assertThatThrownBy( () -> sources.addManagedClassName( "example.package-info" ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "addPackageDescriptor(\"example\")" );
		assertThatThrownBy( () -> sources.addManagedClassName( "module-info" ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "declared module name" );
	}

	@Test
	void explicitAndCompleteConfigurationViewsRemainSeparate() {
		final var configuration = new PersistenceConfiguration( "unit" )
				.managedClass( String.class ).managedPackageDescriptor( "explicit" ).managedModuleDescriptor( "explicit.module" );
		final var source = MappingSources.from( configuration );
		assertThat( source.managedClasses() ).containsExactly( String.class );
		assertThat( source.packageNames() ).containsExactly( "explicit" );
		assertThat( source.moduleNames() ).containsExactly( "explicit.module" );
		final var unit = new PersistenceUnitInfoAdapter() {
			@Override public List<String> getManagedClassNames() { return List.of( String.class.getName() ); }
			@Override public List<String> getAllClassNames() { return List.of( String.class.getName(), Integer.class.getName() ); }
			@Override public List<String> getAllPackageDescriptors() { return List.of( "explicit", "scanned" ); }
			@Override public List<String> getAllModuleDescriptors() { return List.of( "explicit.module", "scanned.module" ); }
		};
		final var container = MappingSources.from( new PersistenceUnitInfoDescriptor( unit ) );
		assertThat( container.managedClassNames() ).containsExactly( String.class.getName(), Integer.class.getName() );
		assertThat( container.packageNames() ).containsExactly( "explicit", "scanned" );
		assertThat( container.moduleNames() ).containsExactly( "explicit.module", "scanned.module" );
	}


	@Test
	void resolvedTypesRetainSuppliedDetailsAndSeparateDescriptors(@TempDir Path directory) throws Exception {
		final var module = TestModule.load( ShrinkWrap.create( JavaArchive.class, "inventory.jar" )
				.addClass( org.hibernate.orm.test.boot.models.inventory.UnlistedEntity.class ), """
				/// @author Steve Ebersole
				@org.hibernate.annotations.FilterDef(name = "moduleInventoryFilter")
				module test.inventory {}
				""", directory, FilterDef.class );
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var buildingContext = new MetadataBuildingContextTestingImpl( registry );
			final var context = buildingContext.getModelsContext();
			final var details = new JdkClassDetails( SampleType.class, context );
			final var dynamic = new DynamicClassDetails( "DynamicModel", context );
			final var sources = new MappingSources().addClassDetails( details ).addClassDetails( dynamic )
					.addManagedClass( SampleType.class ).addManagedClassName( SampleType.class.getName() )
					.addPackageDescriptor( "org.hibernate.orm.test.boot.models.inventory" ).addModule( module.module() );
			final var javaMapping = new JaxbEntityMappingsImpl();
			final var xmlEntity = new org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl();
			final var xmlClassName = "org.hibernate.orm.test.boot.models.inventory.UnlistedEntity";
			xmlEntity.setClazz( xmlClassName );
			xmlEntity.setName( "XmlOnlyEntity" );
			javaMapping.getEntities().add( xmlEntity );
			sources.addMappingResource( "mappings/models/dynamic/dynamic-simple.xml" )
					.addXmlMappingSource( (binder, loading, consumer) -> consumer.accept(
							new Binding<>( javaMapping, new Origin( SourceType.OTHER, "xml-only-java-type" ) ) ) );
			final var settings = SettingsResolver.resolveMappingSettings(
					SettingsResolver.resolveBootstrapSettings( Map.of() ), jakarta.persistence.FetchType.EAGER );
			final var prepared = PreparedMappingSources.from( sources, new MappingSourcePreparationContext( context, registry ), settings );
			assertThat( prepared.managedClassDetails() ).containsExactly( details, dynamic );
			assertThat( prepared.packageDetails() ).extracting( ClassDetails::getName )
					.containsExactly( "org.hibernate.orm.test.boot.models.inventory.package-info" );
			assertThat( prepared.moduleDetails() ).hasSize( 1 );
			assertThat( prepared.moduleDetails().iterator().next().getDirectAnnotationUsage( FilterDef.class ).name() )
					.isEqualTo( "moduleInventoryFilter" );
			assertThat( context.getClassDetailsRegistry().findClassDetails( SampleType.class.getName() ) ).isSameAs( details );
			final var categorized = org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer.categorize(
					prepared, buildingContext );
			assertThat( categorized.getSourceClasses() ).containsKey( xmlClassName ).containsKey( "SimpleEntity" );
			assertThat( categorized.getSourceClasses().keySet() ).noneMatch( name -> name.endsWith( "package-info" ) );
			assertThat( categorized.getEntityHierarchies() ).extracting( hierarchy -> hierarchy.getRoot().getJpaEntityName() )
					.containsExactlyInAnyOrder( "XmlOnlyEntity", "SimpleEntity" );
			assertThat( context.getClassDetailsRegistry().findClassDetails( SampleType.class.getName() ) ).isSameAs( details );
			assertThat( context.getClassDetailsRegistry().findClassDetails( "DynamicModel" ) ).isSameAs( dynamic );
			assertThatThrownBy( () -> sources.addClassDetails( new JdkClassDetails( SampleType.class, context ) ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting ClassDetails" );
		}
	}

	@Test
	void descriptorRegistrationsSurviveArchiveRestoration(@TempDir Path directory) throws Exception {
		final var module = TestModule.load( ShrinkWrap.create( JavaArchive.class, "archive-inventory.jar" )
				.addClass( SampleType.class ), """
				/// @author Steve Ebersole
				@org.hibernate.annotations.FilterDef(name = "moduleArchiveFilter", defaultCondition = "2=2")
				module test.archiveinventory {}
				""", directory, FilterDef.class );
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build() ) {
			final var metadata = MetadataBuildingHelper.buildMetadata( registry,
					new MappingSources().addManagedClass( org.hibernate.orm.test.boot.models.inventory.UnlistedEntity.class )
							.addPackageDescriptor( "org.hibernate.orm.test.boot.models.inventory" ).addModule( module.module() ) );
			final var bytes = new java.io.ByteArrayOutputStream();
			org.hibernate.boot.serial.MetadataSerialization.serialize( metadata ).writeTo( bytes );
			final var restored = org.hibernate.boot.serial.MetadataSerialization.read(
					new java.io.ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			assertThat( restored.getFilterDefinitions() ).containsKeys( "inventoryFilter", "moduleArchiveFilter" );
			assertThat( restored.getFilterDefinitions().get( "inventoryFilter" ).getDefaultFilterCondition() ).isEqualTo( "1=1" );
			assertThat( restored.getFilterDefinitions().get( "moduleArchiveFilter" ).getDefaultFilterCondition() ).isEqualTo( "2=2" );
		}
	}

	@Test
	void enhancementCandidatesAreNamesOnlyAndRejectDescriptors() {
		final var invalidUnit = new PersistenceUnitInfoAdapter() {
			@Override
			public List<String> getManagedClassNames() { return List.of( "example.package-info" ); }
			@Override
			public List<String> getAllClassNames() { return List.of(); }
		};
		assertThatThrownBy( () -> new HibernatePersistenceProvider().getClassTransformer( invalidUnit, Map.of() ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "getManagedPackageDescriptors()" );
		assertThat( EnhancementCandidates.forContainer( List.of( "not.loaded.Entity", "not.loaded.Entity", "xml.Only" ) ) )
				.containsExactly( "not.loaded.Entity", "xml.Only" );
		assertThatThrownBy( () -> EnhancementCandidates.forContainer( List.of( "example.package-info" ) ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "getAllPackageDescriptors()" );

	}

	@Test
	void xmlDescriptorsAndLegacyClassDiagnostics(@TempDir Path directory) throws Exception {
		final var xml = Files.createDirectories( directory.resolve( "META-INF" ) ).resolve( "persistence.xml" );
		final var parser = PersistenceXmlParser.create();
		for ( var version : List.of( "3.2", "4.0" ) ) {
			Files.writeString( xml, """
					<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="%s">
					<persistence-unit name="unit"><class>com.acme.package-info</class></persistence-unit>
					</persistence>
					""".formatted( version ) );
			assertThatThrownBy( () -> parser.parse( List.of( xml.toUri().toURL() ) ) )
					.hasMessageContaining( "<class>com.acme.package-info</class> is illegal, use <package-descriptor>com.acme</package-descriptor> instead" )
					.hasMessageContaining( "unit" ).hasMessageContaining( "persistence.xml" );
		}
		Files.writeString( xml, """
				<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="4.0">
				<persistence-unit name="unit"><package-descriptor>com.acme</package-descriptor>
					<module-descriptor>com.acme.module</module-descriptor></persistence-unit>
				</persistence>
				""" );
		final var descriptor = parser.parse( List.of( xml.toUri().toURL() ) ).get( "unit" );
		assertThat( descriptor.getManagedClassNames() ).isEmpty();
		assertThat( descriptor.getManagedPackageDescriptors() ).containsExactly( "com.acme" );
		assertThat( descriptor.getManagedModuleDescriptors() ).containsExactly( "com.acme.module" );
	}
}
