/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;

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
import static org.hibernate.cfg.AvailableSettings.HBM_XML_FILES;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;
import static org.hibernate.cfg.AvailableSettings.CFG_XML_FILE;
import static org.hibernate.jpa.boot.spi.JpaSettings.METADATA_BUILDER_CONTRIBUTOR;

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
			final var builder = builder( configuration );
			try {
				assertThat( builder.getManagedResources().getXmlMappingBindings() ).hasSize( 1 );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
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
			final var builder = builder( configuration );
			try {
				final var resources = builder.getManagedResources();
				assertThat( resources.getAnnotatedClassReferences() ).containsExactly( JpaPreparationTest.DeclaredEntity.class );
				assertThat( resources.getAnnotatedClassNames() ).containsExactly( JpaPreparationTest.DeclaredEntity.class.getName() );
				assertThat( resources.getAnnotatedPackageNames() ).containsExactly( "explicit.package" );
				assertThat( resources.getAnnotatedModuleNames() ).containsExactly( "explicit.module" );
				assertThat( resources.getXmlMappingBindings() ).hasSize( 2 );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void explicitDefaultAndLegacyMappingsRetainOrder(boolean hibernate, boolean xmlEnabled) throws Exception {
		Files.createDirectories( directory.resolve( "META-INF" ) );
		Files.writeString( directory.resolve( "META-INF/orm.xml" ), EMPTY_MAPPING );
		Files.writeString( directory.resolve( "explicit.xml" ), EMPTY_MAPPING );
		Files.writeString( directory.resolve( "legacy.xml" ), EMPTY_MAPPING );
		try ( var loader = loader() ) {
			final var configuration = configuration( hibernate, xmlEnabled, loader )
					.mappingFile( "explicit.xml" ).mappingFile( "META-INF/orm.xml" )
					.property( HBM_XML_FILES, "legacy.xml" );
			final var builder = builder( configuration );
			try {
				assertThat( builder.getManagedResources().getXmlMappingBindings() )
						.hasSize( xmlEnabled ? 3 : 0 );
				if ( xmlEnabled ) {
					assertThat( builder.getManagedResources().getXmlMappingBindings() )
							.extracting( binding -> binding.getOrigin().getName() )
							.containsExactly( "explicit.xml", "META-INF/orm.xml", "legacy.xml" );
				}
				assertThat( builder.getConfigurationValues().containsKey( HBM_XML_FILES ) ).isEqualTo( !xmlEnabled );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void explicitXmlFailuresPrecedeContributorsUnlessDisabled(boolean hibernate, boolean malformed) throws Exception {
		if ( malformed ) {
			Files.writeString( directory.resolve( "broken.xml" ), "<not-valid" );
		}
		try ( var loader = loader() ) {
			final var called = new AtomicBoolean();
			final var configuration = configuration( hibernate, true, loader ).mappingFile( "broken.xml" )
					.property( METADATA_BUILDER_CONTRIBUTOR, (MetadataBuilderContributor) ignored -> called.set( true ) );
			assertThatThrownBy( () -> builder( configuration ) ).isInstanceOf( MappingException.class );
			assertThat( called ).isFalse();
			configuration.property( XML_MAPPING_ENABLED, false );
			final var builder = builder( configuration );
			try {
				assertThat( called ).isTrue();
				assertThat( builder.getManagedResources().getXmlMappingBindings() ).isEmpty();
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void configurationXmlFailuresFollowContributorsOnlyWhenEnabled(boolean hibernate, boolean xmlEnabled) throws Exception {
		Files.writeString( directory.resolve( "hibernate.cfg.xml" ), """
				<!DOCTYPE hibernate-configuration PUBLIC
					"-//Hibernate/Hibernate Configuration DTD 3.0//EN"
					"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
				<hibernate-configuration><session-factory>
					<mapping resource="missing.xml"/>
				</session-factory></hibernate-configuration>
				""" );
		try ( var loader = loader() ) {
			final var called = new AtomicBoolean();
			final var configuration = configuration( hibernate, xmlEnabled, loader )
					.property( CFG_XML_FILE, "hibernate.cfg.xml" )
					.property( METADATA_BUILDER_CONTRIBUTOR, (MetadataBuilderContributor) ignored -> called.set( true ) );
			if ( xmlEnabled ) {
				assertThatThrownBy( () -> builder( configuration ) ).isInstanceOf( MappingException.class );
			}
			else {
				final var builder = builder( configuration );
				try {
					assertThat( builder.getManagedResources().getXmlMappingBindings() ).isEmpty();
				}
				finally {
					StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
				}
			}
			assertThat( called ).isTrue();
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void nativeXmlSkipsBindingWhenDisabled(boolean configuration, boolean malformed) throws Exception {
		if ( malformed ) {
			Files.writeString( directory.resolve( "broken.xml" ), "<not-valid" );
		}
		try ( var loader = loader();
				var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				var registry = new StandardServiceRegistryBuilder( bootstrap )
						.applySetting( XML_MAPPING_ENABLED, false ).build() ) {
			if ( configuration ) {
				final var cfg = new Configuration( bootstrap ).setProperty( XML_MAPPING_ENABLED, "false" );
				assertThat( cfg.addResource( "broken.xml" ) ).isSameAs( cfg );
			}
			else {
				final var sources = new MetadataSources( registry );
				sources.addResource( "broken.xml" );
				assertThat( sources.getMappingXmlBindings() ).isEmpty();
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

	private static EntityManagerFactoryBuilderImpl builder(PersistenceConfiguration configuration) {
		return configuration instanceof HibernatePersistenceConfiguration hibernate
				? new EntityManagerFactoryBuilderImpl( hibernate )
				: new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
	}
}
