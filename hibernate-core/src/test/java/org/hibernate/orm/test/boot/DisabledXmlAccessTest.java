/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.hibernate.boot.model.process.internal.MappingSourceHelper;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

/// Disabled mapping XML must not access resources or consume caller-owned streams.
///
/// @author Steve Ebersole
@BaseUnitTest
class DisabledXmlAccessTest {
	private static final String XML = """
			<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2"/>
			""";

	@TempDir
	Path directory;

	@Test
	void configurationMappingsKeepClassesAndPackagesWhileSkippingXml() throws Exception {
		final var cfgFile = directory.resolve( "hibernate.cfg.xml" );
		Files.writeString( cfgFile, """
				<!DOCTYPE hibernate-configuration PUBLIC
					"-//Hibernate/Hibernate Configuration DTD 3.0//EN"
					"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
				<hibernate-configuration><session-factory>
					<mapping class="java.lang.String"/>
					<mapping package="java.lang"/>
					<mapping resource="missing-disabled.xml"/>
					<mapping file="missing-disabled.xml"/>
					<mapping jar="missing-disabled.jar"/>
				</session-factory></hibernate-configuration>
				""" );
		try ( var registry = new StandardServiceRegistryBuilder().configure( cfgFile.toFile() )
				.applySetting( XML_MAPPING_ENABLED, false ).build() ) {
			final var sources = new MetadataSources( registry );
			MappingSourceHelper.applyConfigurationMappings( sources, registry );
			assertThat( sources.getAnnotatedClassNames() ).containsExactly( "java.lang.String" );
			assertThat( sources.getAnnotatedPackages() ).containsExactly( "java.lang" );
			assertThat( sources.getMappingXmlBindings() ).isEmpty();
			assertThat( sources.getHbmXmlBindings() ).isEmpty();
		}
	}

	@Test
	void disabledResourceIsNotLookedUp() {
		final var loader = new ClassLoader( getClass().getClassLoader() ) {
			@Override
			public URL getResource(String name) {
				if ( name.equals( "missing-disabled.xml" ) ) {
					throw new AssertionError( "Looked up disabled resource" );
				}
				return super.getResource( name );
			}
		};
		try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				var registry = new StandardServiceRegistryBuilder( bootstrap ).applySetting( XML_MAPPING_ENABLED, false ).build() ) {
			new MetadataSources( registry ).addResource( "missing-disabled.xml" );
		}
	}

	@Test
	void metadataSourcesSkipEveryXmlInputForm() throws Exception {
		try ( var registry = new StandardServiceRegistryBuilder().applySetting( XML_MAPPING_ENABLED, false ).build() ) {
			final var sources = new MetadataSources( registry );
			final var file = inaccessibleFile();
			sources.addResource( "missing-disabled.xml" );
			sources.addFile( file ).addFile( "missing-disabled.xml" );
			sources.addURL( inaccessibleUrl() );
			sources.addDirectory( file ).addJar( file );
			sources.addCacheableFile( file ).addCacheableFile( file, file );
			sources.addCacheableFileStrictly( file ).addCacheableFileStrictly( file, file );
			sources.addInputStream( inaccessibleStream() );
			sources.addInputStream( new InputStreamAccess() {
				@Override
				public String getStreamName() {
					return "disabled";
				}

				@Override
				public InputStream accessInputStream() {
					throw new AssertionError( "Acquired disabled stream" );
				}
			} );
			assertThat( sources.getMappingXmlBindings() ).isEmpty();
			assertThat( sources.getHbmXmlBindings() ).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void configurationUsesCurrentPropertiesIncludingWithSuppliedSources(boolean suppliedSources) throws Exception {
		try ( var bootstrap = new BootstrapServiceRegistryBuilder().build() ) {
			final var sources = new MetadataSources( bootstrap );
			final var cfg = suppliedSources ? new Configuration( sources ) : new Configuration( bootstrap );
			cfg.setProperty( XML_MAPPING_ENABLED, false );
			final var file = inaccessibleFile();
			cfg.addResource( "missing-disabled.xml" ).addFile( file ).addURL( inaccessibleUrl() );
			cfg.addJar( file ).addDirectory( file );
			cfg.addCacheableFile( file ).addCacheableFileStrictly( file );
			cfg.addInputStream( inaccessibleStream() );
			cfg.getProperties().put( XML_MAPPING_ENABLED, true );
			assertThatThrownBy( () -> cfg.addResource( "missing-disabled.xml" ) ).isInstanceOf( MappingException.class );
			cfg.addInputStream( new ByteArrayInputStream( XML.getBytes( StandardCharsets.UTF_8 ) ) );
			if ( suppliedSources ) {
				assertThat( sources.getMappingXmlBindings() ).hasSize( 1 );
				cfg.setProperty( XML_MAPPING_ENABLED, false );
				assertThat( sources.getMappingXmlBindings() ).hasSize( 1 );
			}
		}
	}

	@Test
	void bootstrapRegistryWithoutSettingsStillBindsImmediately() {
		try ( var bootstrap = new BootstrapServiceRegistryBuilder().build() ) {
			assertThatThrownBy( () -> new MetadataSources( bootstrap ).addResource( "missing-enabled.xml" ) )
					.isInstanceOf( MappingException.class );
		}
	}

	private static InputStream inaccessibleStream() {
		return new InputStream() {
			@Override
			public int read() {
				throw new AssertionError( "Read disabled stream" );
			}

			@Override
			public void close() {
				throw new AssertionError( "Closed caller-owned stream" );
			}
		};
	}

	private static URL inaccessibleUrl() throws Exception {
		return new URL( null, "test:disabled", new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL url) {
				throw new AssertionError( "Opened disabled URL" );
			}
		} );
	}

	private static File inaccessibleFile() {
		return new File( "disabled.xml" ) {
			@Override
			public boolean exists() {
				throw new AssertionError( "Accessed disabled file" );
			}

			@Override
			public long lastModified() {
				throw new AssertionError( "Accessed disabled cache" );
			}

			@Override
			public File[] listFiles() {
				throw new AssertionError( "Traversed disabled directory" );
			}
		};
	}
}
