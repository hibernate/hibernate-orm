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
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.process.internal.EnhancementCandidates;
import org.hibernate.boot.model.process.internal.ManagedResourcesBuilder;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.xml.internal.PersistenceUnitMetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.scan.internal.ScanningResultImpl;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.jdk.JdkClassDetails;
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

			final var builder = new EntityManagerFactoryBuilderImpl( configuration );
			try {
				assertThat( calls.get() ).isEqualTo( withBoundary ? 1 : 0 );
				if ( withBoundary ) {
					assertThat( builder.getManagedResources().getAnnotatedClassNames() ).containsExactly( SampleType.class.getName() );
					assertThat( builder.getManagedResources().getAnnotatedPackageNames() ).containsExactly( "example" );
					assertThat( builder.getManagedResources().getAnnotatedModuleNames() ).containsExactly( "java.base" );
				}
				assertThat( builder.getManagedResources().getXmlMappingBindings() ).isEmpty();
			}
			finally {
				builder.cancel();
			}
		}
	}

	@Test
	void descriptorOnlyMetadataContributesWithoutEnrollingPackageClasses() {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var metadata = new MetadataSources( registry )
					.addPackageDescriptor( "org.hibernate.orm.test.boot.models.inventory" ).buildMetadata();
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
		assertThatThrownBy( () -> new ManagedResourcesBuilder().addClass( SampleType.class ).addClass( alternate ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( SampleType.class.getName() ).hasMessageContaining( "class loaders" );
	}

	@Test
	void bothTransformerPathsUseCompleteContainerInventory() {
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
			final var provider = new HibernatePersistenceProvider();
			if ( direct ) {
				assertThat( provider.getClassTransformer( unit, Map.of() ) ).isNotNull();
				assertThat( provider.getClassTransformer( unit, Map.of() ) ).isNull();
			}
			else {
				final var builder = new EntityManagerFactoryBuilderImpl(
						new PersistenceUnitInfoDescriptor( unit ),
						ServiceRegistryUtil.createBaseSettings() );
				try {
					assertThat( registered ).hasSize( 1 );
				}
				finally { builder.cancel(); }
			}
			assertThat( reads ).contains( entityName.replace( '.', '/' ) + ".class" );
			assertThat( reads ).noneMatch( name -> name.endsWith( "package-info.class" ) || name.endsWith( "module-info.class" ) );
		}
	}

	@Test
	void disabledXmlIsNotProcessedFromAnExternalBatch() {
		try ( var registry = new StandardServiceRegistryBuilder().applySetting( MappingSettings.XML_MAPPING_ENABLED, false ).build() ) {
			final var options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( registry );
			final var bootstrap = new BootstrapContextImpl( registry, options );
			options.setBootstrapContext( bootstrap );
			final var xml = new JaxbEntityMappingsImpl();
			final var entity = new JaxbEntityImpl();
			entity.setClazz( "not.loaded.DisabledXmlEntity" );
			xml.getEntities().add( entity );
			final var resources = new ManagedResourcesBuilder()
					.addXmlBinding( new Binding<>( xml, new Origin( SourceType.OTHER, "disabled" ) ) ).build();
			final var source = MetadataBuildingProcess.processManagedResources( resources, bootstrap, options.getMappingDefaults(),
					bootstrap.getModelsContext(), new PersistenceUnitMetadataImpl(), new GlobalRegistrationsImpl( bootstrap.getModelsContext(), bootstrap ) );
			assertThat( source.getManagedTypes() ).isEmpty();
		}
	}

	@Test
	void snapshotsRetainIdentityAndExplicitXmlMultiplicity() {
		final var xml = new Binding<>( new JaxbEntityMappingsImpl(), new Origin( SourceType.OTHER, "test" ) );
		final var builder = new ManagedResourcesBuilder().addClass( String.class ).addClass( String.class )
				.addClassName( "example.NotLoaded" ).addPackageDescriptor( "example" )
				.addModuleDescriptor( "example.module" ).addXmlBinding( xml ).addXmlBinding( xml )
				.addQueryImport( "Alias", String.class );
		final var first = builder.build();
		builder.addClass( Integer.class ).addPackageDescriptor( "another" ).addQueryImport( "Alias", Integer.class );
		assertThat( first.getAnnotatedClassReferences() ).containsExactly( String.class );
		assertThat( first.getAnnotatedClassNames() ).containsExactly( "example.NotLoaded" );
		assertThat( first.getAnnotatedPackageNames() ).containsExactly( "example" );
		assertThat( first.getXmlMappingBindings() ).containsExactly( xml, xml );
		assertThat( first.getExtraQueryImports() ).containsEntry( "Alias", String.class );
		assertThat( builder.build().getExtraQueryImports() ).containsEntry( "Alias", Integer.class );
		assertThatThrownBy( () -> first.getAnnotatedClassNames().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> first.getAnnotatedClassReferences().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> first.getXmlMappingBindings().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> first.getExtraQueryImports().clear() ).isInstanceOf( UnsupportedOperationException.class );
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
		assertThat( ordinary.getPackage().isRealClass() ).isFalse();

		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( registry );
			final var bootstrap = new BootstrapContextImpl( registry, options );
			options.setBootstrapContext( bootstrap );
			final var resources = new ManagedResourcesBuilder().addClassName( SampleType.class.getName() )
					.addPackageDescriptor( packageName ).build();
			final var source = MetadataBuildingProcess.processManagedResources( resources, bootstrap, options.getMappingDefaults(),
					context, new PersistenceUnitMetadataImpl(), new GlobalRegistrationsImpl( context, bootstrap ) );
			assertThat( source.getPackageDescriptors() ).containsExactly( packageDetails );
			for ( var missingPackage : new String[] { "does.not.exist", SampleType.class.getPackageName(), SampleType.class.getName() } ) {
				final var explicit = new ManagedResourcesBuilder().addPackageDescriptor( missingPackage ).build();
				assertThatThrownBy( () -> MetadataBuildingProcess.processManagedResources(
						explicit, bootstrap, options.getMappingDefaults(), context,
						new PersistenceUnitMetadataImpl(), new GlobalRegistrationsImpl( context, bootstrap ) ) )
						.isInstanceOf( UnknownClassException.class )
						.hasMessageContaining( missingPackage + ".package-info" );
			}

			assertThat( source.getManagedJavaTypes() ).containsExactly( ordinary );
			assertThat( source.getDynamicManagedTypes() ).isEmpty();
			assertThat( source.getManagedTypes() ).containsExactly( ordinary );
			assertThat( EnhancementCandidates.forResources( resources ) ).containsOnlyKeys( SampleType.class.getName() );
		}
	}

	@Test
	void nativeDescriptorsAndAliasesAgree() {
		final var sources = new MetadataSources();
		assertThat( sources.addPackageDescriptor( "example." ) ).isSameAs( sources );
		sources.addPackage( "example" ).addPackageDescriptor( "example" );
		sources.addPackageDescriptor( String.class.getPackage() ).addPackage( String.class.getPackage() );
		sources.addModuleDescriptor( String.class.getModule() ).addModule( String.class.getModule() );
		assertThat( sources.getAnnotatedPackages() ).containsExactly( "example", "java.lang" );
		assertThat( sources.getAnnotatedModuleNames() ).containsExactly( "java.base" );
		assertThatThrownBy( () -> sources.addAnnotatedClassName( "example.package-info" ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "addPackageDescriptor(\"example\")" );
		assertThatThrownBy( () -> sources.addAnnotatedClassName( "module-info" ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "declared module name" );
	}

	@Test
	void explicitAndCompleteConfigurationViewsRemainSeparate() {
		final var configuration = new PersistenceConfiguration( "unit" )
				.managedClass( String.class ).managedPackageDescriptor( "explicit" ).managedModuleDescriptor( "explicit.module" );
		final var discovery = new ScanningResultImpl( Set.of( "scanned.module" ), Set.of( "scanned", "explicit" ),
				Set.of( String.class.getName(), Integer.class.getName() ), Set.of() );
		final var descriptor = new PersistenceConfigurationDescriptor( configuration, discovery );
		assertThat( descriptor.getManagedClassNames() ).containsExactly( String.class.getName() );
		assertThat( descriptor.getAllClassNames() ).containsExactly( String.class.getName(), Integer.class.getName() );
		assertThat( descriptor.getManagedPackageDescriptors() ).containsExactly( "explicit" );
		assertThat( descriptor.getAllPackageDescriptors() ).containsExactlyInAnyOrder( "explicit", "scanned" );
		assertThat( descriptor.getAllModuleDescriptors() ).containsExactly( "explicit.module", "scanned.module" );
		assertThat( new PersistenceConfigurationDescriptor( configuration ).getAllClassNames() )
				.containsExactly( String.class.getName() );
		final var builder = new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
		try {
			assertThat( builder.getManagedResources().getAnnotatedClassReferences() ).containsExactly( String.class );
		}
		finally {
			builder.cancel();
		}
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
			final var options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( registry );
			final var bootstrap = new BootstrapContextImpl( registry, options );
			options.setBootstrapContext( bootstrap );
			final var context = bootstrap.getModelsContext();
			// Name lookup alone cannot find a module in a child layer.
			final var moduleDetails = context.getModuleDetailsRegistry().resolveModuleDetails( module.module() );
			final var details = new JdkClassDetails( SampleType.class, context );
			final var dynamic = new DynamicClassDetails( "DynamicModel", context );
			final var xmlSources = new MetadataSources( registry ).addResource( "mappings/models/dynamic/dynamic-simple.xml" );
			final var javaMapping = new JaxbEntityMappingsImpl();
			final var xmlEntity = new JaxbEntityImpl();
			final var xmlClassName = "org.hibernate.orm.test.boot.models.inventory.UnlistedEntity";
			xmlEntity.setClazz( xmlClassName );
			javaMapping.getEntities().add( xmlEntity );
			final var resourceBuilder = new ManagedResourcesBuilder();
			xmlSources.getMappingXmlBindings().forEach( resourceBuilder::addXmlBinding );
			resourceBuilder.addXmlBinding( new Binding<>( javaMapping, new Origin( SourceType.OTHER, "xml-only-java-type" ) ) );
			final var resources = resourceBuilder.addClass( SampleType.class ).addClassName( SampleType.class.getName() )
					.addClassDetails( details ).addClassDetails( details ).addClassDetails( dynamic )
					.addPackageDescriptor( "org.hibernate.orm.test.boot.models.inventory" )
					.addModuleDescriptor( module.module().getName() ).build();
			final var source = MetadataBuildingProcess.processManagedResources( resources, bootstrap, options.getMappingDefaults(),
					context, new PersistenceUnitMetadataImpl(), new GlobalRegistrationsImpl( context, bootstrap ) );
			assertThat( source.getManagedJavaTypes() ).contains( details );
			assertThat( source.getManagedJavaTypes() ).extracting( ClassDetails::getName ).containsExactly( SampleType.class.getName(), xmlClassName );
			assertThat( source.getDynamicManagedTypes() ).contains( dynamic );
			assertThat( source.getDynamicManagedTypes() ).extracting( ClassDetails::getName ).containsExactly( "DynamicModel", "SimpleEntity" );
			assertThat( source.getPackageDescriptors() ).extracting( ClassDetails::getName )
					.containsExactly( "org.hibernate.orm.test.boot.models.inventory.package-info" );
			assertThat( source.getModuleDescriptors() ).extracting( descriptor -> descriptor.name() ).containsExactly( module.module().getName() );
			assertThat( source.getModuleDescriptors().get( 0 ).target() ).isSameAs( moduleDetails );
			assertThat( moduleDetails.getDirectAnnotationUsage( FilterDef.class ).name() ).isEqualTo( "moduleInventoryFilter" );
			assertThat( source.getManagedTypes() ).hasSize( 4 ).contains( details, dynamic );
			assertThat( source.getManagedTypes() ).noneMatch( type -> type.getName().endsWith( "package-info" ) );
			assertThat( context.getClassDetailsRegistry().findClassDetails( SampleType.class.getName() ) ).isSameAs( details );
			assertThatThrownBy( () -> new ManagedResourcesBuilder().addClassDetails( details )
					.addClassDetails( new JdkClassDetails( SampleType.class, context ) ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting ClassDetails" );
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
		final var resources = new ManagedResourcesBuilder().addClassName( "not.loaded.Entity" )
				.addPackageDescriptor( "example" ).addModuleDescriptor( "example.module" ).build();
		assertThat( EnhancementCandidates.forResources( resources ) ).containsOnlyKeys( "not.loaded.Entity" );
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
