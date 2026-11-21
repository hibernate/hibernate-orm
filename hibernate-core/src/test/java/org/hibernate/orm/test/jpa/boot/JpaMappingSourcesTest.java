package org.hibernate.orm.test.jpa.boot;

import jakarta.persistence.FetchType;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.ConfigurationMappingProcessor;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;


import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

/// Characterizes source ordering, automatic duplicates, and XML failure boundaries.
/// Disabled XML is skipped before mapping resources are accessed.
///
/// @author Steve Ebersole
@BaseUnitTest
class JpaMappingSourcesTest {
	private static final String EMPTY_MAPPING = """
			<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2"/>
			""";

	@TempDir
	Path directory;

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void defaultAndScannedMappingsDoNotRepeatExplicitOrAutomaticMappings(boolean explicit) throws Exception {
		Files.createDirectories( directory.resolve( "META-INF" ) );
		Files.writeString( directory.resolve( "META-INF/orm.xml" ), EMPTY_MAPPING );
		try ( var loader = loader() ) {
			final var configuration = new HibernatePersistenceConfiguration( "overlap", directory.toUri().toURL() )
					.property( CLASSLOADERS, List.of( loader ) )
					.property( DIALECT, H2Dialect.class.getName() ).property( ALLOW_METADATA_ON_BOOT, false );
			if ( explicit ) {
				configuration.mappingFile( "META-INF/orm.xml" );
			}
			try ( var registry = registry( configuration ); ) {
				final var resources = prepare( configuration, registry );
				assertThat( resources.xmlMappings() ).hasSize( 1 );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void structuralDeclarationsAndRepeatedExplicitMappingsArePreserved(boolean hibernate) throws Exception {
		Files.writeString( directory.resolve( "explicit.xml" ), EMPTY_MAPPING );
		try ( var loader = loader() ) {
			final var configuration = configuration( hibernate, true, loader )
					.managedClass( JpaPreparationTest.DeclaredEntity.class )
					.managedPackageDescriptor( "explicit.package" ).managedModuleDescriptor( "explicit.module" )
					.mappingFile( "explicit.xml" ).mappingFile( "explicit.xml" );
			try ( var registry = registry( configuration ); ) {
				final var resources = prepare( configuration( hibernate, true, loader ).mappingFile( "explicit.xml" ).mappingFile( "explicit.xml" ), registry );
				final var declarations = ConfigurationMappingProcessor.declared( configuration );
				assertThat( declarations.managedClasses() ).containsExactly( JpaPreparationTest.DeclaredEntity.class );
				assertThat( declarations.managedClasses().stream().map( Class::getName ).toList() ).containsExactly( JpaPreparationTest.DeclaredEntity.class.getName() );
				assertThat( declarations.packageNames() ).containsExactly( "explicit.package" );
				assertThat( declarations.moduleNames() ).containsExactly( "explicit.module" );
				assertThat( resources.xmlMappings() ).hasSize( 2 );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void explicitMappingsRetainOrder(boolean hibernate, boolean xmlEnabled) throws Exception {
		Files.createDirectories( directory.resolve( "META-INF" ) );
		Files.writeString( directory.resolve( "META-INF/orm.xml" ), EMPTY_MAPPING );
		Files.writeString( directory.resolve( "explicit.xml" ), EMPTY_MAPPING );
		Files.writeString( directory.resolve( "legacy.xml" ), EMPTY_MAPPING );
		try ( var loader = loader() ) {
			final var configuration = configuration( hibernate, xmlEnabled, loader )
					.mappingFile( "explicit.xml" ).mappingFile( "META-INF/orm.xml" ).mappingFile( "legacy.xml" );
			try ( var registry = registry( configuration ); ) {
				final var resources = prepare( configuration, registry );
				assertThat( resources.xmlMappings() )
						.hasSize( xmlEnabled ? 3 : 0 );
				if ( xmlEnabled ) {
					assertThat( resources.xmlMappings() )
							.extracting( binding -> binding.getOrigin().getName() )
							.containsExactly( "explicit.xml", "META-INF/orm.xml", "legacy.xml" );
				}
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void explicitXmlFailuresAreDeferredUntilPreparationUnlessDisabled(boolean hibernate, boolean malformed) throws Exception {
		if ( malformed ) {
			Files.writeString( directory.resolve( "broken.xml" ), "<not-valid" );
		}
		try ( var loader = loader() ) {
			final var configuration = configuration( hibernate, true, loader ).mappingFile( "broken.xml" );
			assertThat( ConfigurationMappingProcessor.declared( configuration ).xmlMappingSources() ).hasSize( 1 );
			try ( var registry = registry( configuration ) ) {
				assertThatThrownBy( () -> prepare( configuration, registry ) ).isInstanceOf( MappingException.class );
				configuration.property( XML_MAPPING_ENABLED, false );
				assertThat( prepare( configuration, registry ).xmlMappings() ).isEmpty();
			}
		}
	}

	private URLClassLoader loader() throws Exception {
		return new URLClassLoader( new URL[] { directory.toUri().toURL() }, getClass().getClassLoader() );
	}

	private static PersistenceConfiguration configuration(boolean hibernate, boolean xmlEnabled, ClassLoader loader) {
		final PersistenceConfiguration configuration = hibernate
				? new HibernatePersistenceConfiguration( "sources" ) : new PersistenceConfiguration( "sources" );
		return configuration.property( CLASSLOADERS, List.of( loader ) )
				.property( XML_MAPPING_ENABLED, xmlEnabled )
				.property( DIALECT, H2Dialect.class.getName() ).property( ALLOW_METADATA_ON_BOOT, false );
	}

	private static StandardServiceRegistry registry(PersistenceConfiguration configuration) {
		final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader(
				(ClassLoader) ((List<?>) configuration.properties().get( CLASSLOADERS )).get( 0 ) ).build();
		return new StandardServiceRegistryBuilder( bootstrap ).applySettings( configuration.properties() ).build();
	}

	private static PreparedMappingSources prepare(PersistenceConfiguration configuration, StandardServiceRegistry registry) {
		final var context = new MetadataBuildingContextTestingImpl( registry );
		final var preparation = new MappingSourcePreparationContext( context.getModelsContext(), registry );
		final var settings = SettingsResolver.resolveMappingSettings(
				SettingsResolver.resolveBootstrapSettings( configuration.properties() ), FetchType.EAGER );
		return configuration instanceof HibernatePersistenceConfiguration hibernate
				? PreparedMappingSources.from( hibernate, preparation, settings )
				: PreparedMappingSources.from( ConfigurationMappingProcessor.declared( configuration ), preparation, settings );
	}
}
