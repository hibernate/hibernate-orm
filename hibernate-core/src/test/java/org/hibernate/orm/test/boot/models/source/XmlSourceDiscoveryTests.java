/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.pipeline.internal.source.ContributionDiscoveryContext;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.XmlMappingSource;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;

import jakarta.persistence.FetchType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises XML admission separately from type scanning and lazy XML binding.
///
/// @author Steve Ebersole
class XmlSourceDiscoveryTests {
	@TempDir
	Path directory;

	static Stream<Arguments> entryPoints() {
		return Stream.of( false, true ).flatMap( container ->
				Stream.of( false, true ).flatMap( packaged ->
						Stream.of( false, true ).map( exclude -> Arguments.of( container, packaged, exclude ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("entryPoints")
	void admitsOnlyPersistenceUnitMappings(boolean container, boolean packaged, boolean exclude) throws Exception {
		final var rootDirectory = Files.createDirectory( directory.resolve( "root" ) );
		Files.createDirectory( rootDirectory.resolve( "META-INF" ) );
		Files.writeString( rootDirectory.resolve( "META-INF/orm.xml" ), mapping( "Root" ) );
		final var root = packaged ? jar( "root.jar", Map.of( "META-INF/orm.xml", mapping( "Root" ) ) )
				: rootDirectory.toUri().toURL();
		final var referenced = jar( "referenced.jar", Map.of( "META-INF/orm.xml", mapping( "Referenced" ) ) );
		final var unrelated = jar( "unrelated.jar", Map.of(
				"META-INF/orm.xml", mapping( "Unrelated" ), "explicit.xml", mapping( "Explicit" ) ) );
		final var calls = new AtomicInteger();
		final Scanner scanner = new Scanner() {
			@Override
			public ScanningResult scan(URL... boundaries) {
				if ( container ) {
					throw new AssertionError( "Container inventory must not be scanned" );
				}
				calls.incrementAndGet();
				assertThat( boundaries ).containsExactlyElementsOf( exclude ? List.of( referenced ) : List.of( root, referenced ) );
				return ScanningResult.NONE;
			}

			@Override
			public ScanningResult jpaScan(org.hibernate.boot.archive.spi.ArchiveDescriptor archive,
					org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl unit) {
				throw new AssertionError( "Unexpected scan" );
			}
		};
		try (var loader = new URLClassLoader( new URL[] { unrelated, root, referenced }, getClass().getClassLoader() )) {
			final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
			try (var registry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrap ).build()) {
				final var descriptor = new PersistenceUnitInfoDescriptor( new PersistenceUnitInfoAdapter() {
					@Override public URL getPersistenceUnitRootUrl() { return root; }
					@Override public List<URL> getJarFileUrls() { return List.of( referenced ); }
					@Override public boolean excludeUnlistedClasses() { return exclude; }
					@Override public List<String> getMappingFileNames() { return List.of( "explicit.xml" ); }
				} );
				final var adapter = container ? PersistenceUnitSources.container( descriptor ) : PersistenceUnitSources.standalone( descriptor );
				final var settings = SettingsResolver.resolveBootstrapSettings( Map.of( PersistenceSettings.SCANNER, scanner ) );
				final var sources = adapter.collect( settings, new ContributionDiscoveryContext( registry.requireService( ClassLoaderService.class ) ) );
				assertThat( prepare( sources, registry, true ).xmlMappings() )
						.extracting( binding -> binding.getRoot().getEntities().get( 0 ).getClazz() )
						.containsExactly( "example.Explicit", "example.Root", "example.Referenced" );
				assertThat( calls.get() ).isEqualTo( container ? 0 : 1 );
			}
		}
	}

	@Test
	void missingRootDoesNotSearchAmbientClasspath() throws Exception {
		final var unrelated = jar( "unrelated.jar", Map.of( "META-INF/orm.xml", mapping( "Unrelated" ) ) );
		try (var loader = new URLClassLoader( new URL[] { unrelated }, getClass().getClassLoader() )) {
			final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
			try (var registry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrap ).build()) {
				final var sources = PersistenceUnitSources.container( new PersistenceUnitInfoDescriptor( new PersistenceUnitInfoAdapter() ) )
						.collect( SettingsResolver.resolveBootstrapSettings( Map.of() ),
								new ContributionDiscoveryContext( registry.requireService( ClassLoaderService.class ) ) );
				assertThat( prepare( sources, registry, true ).xmlMappings() ).isEmpty();
			}
		}
	}

	@Test
	void preservesBindingOrderAndExplicitDuplicatesAcrossForms() throws Exception {
		final var file = directory.resolve( "mapping.xml" );
		Files.writeString( file, mapping( "Mixed" ) );
		try (var loader = new URLClassLoader( new URL[] { directory.toUri().toURL() }, getClass().getClassLoader() )) {
			final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
			try (var registry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrap ).build()) {
				final var sources = new MappingSources()
						.addXmlMappingSource( XmlMappingSource.fromInputStream( new ByteArrayInputStream( mapping( "Stream" ).getBytes( StandardCharsets.UTF_8 ) ) ) )
						.addMappingUrl( file.toUri().toURL() ).addMappingUri( file.toUri() )
						.addMappingResource( "mapping.xml" ).addMappingResource( "mapping.xml" )
						.addXmlMappingSource( XmlMappingSource.discoveredUri( file.toUri() ) )
						.addXmlMappingSource( XmlMappingSource.discoveredUri( file.toUri() ) );
				final var copied = MappingSources.from( sources );
				assertThat( prepare( copied, registry, true ).xmlMappings() ).hasSize( 5 )
						.extracting( binding -> binding.getOrigin().getType() )
						.containsExactly( org.hibernate.boot.jaxb.SourceType.RESOURCE, org.hibernate.boot.jaxb.SourceType.RESOURCE,
								org.hibernate.boot.jaxb.SourceType.URL, org.hibernate.boot.jaxb.SourceType.URL, org.hibernate.boot.jaxb.SourceType.INPUT_STREAM );
			}
		}
	}

	@Test
	void deduplicatesDiscoveryWithoutSuppressingExplicitDeclarations() throws Exception {
		final var file = directory.resolve( "mapping.xml" );
		Files.writeString( file, mapping( "Discovered" ) );
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var sources = new MappingSources().addXmlMappingSource( XmlMappingSource.discoveredUri( file.toUri() ) )
					.addXmlMappingSource( XmlMappingSource.discoveredUri( file.toUri() ) );
			assertThat( prepare( sources, registry, true ).xmlMappings() ).hasSize( 1 );
			sources.addMappingUri( file.toUri() ).addMappingUri( file.toUri() );
			assertThat( prepare( sources, registry, true ).xmlMappings() ).hasSize( 2 );
		}
	}

	@Test
	void preservesStreamOwnershipAndSkipsDisabledXml() {
		final var opened = new AtomicInteger();
		final var closed = new AtomicInteger();
		final var access = new InputStreamAccess() {
			@Override public String getStreamName() { return "invalid.xml"; }
			@Override public java.io.InputStream accessInputStream() {
				opened.incrementAndGet();
				return new ByteArrayInputStream( "not XML".getBytes( StandardCharsets.UTF_8 ) ) {
					@Override public void close() throws IOException { closed.incrementAndGet(); super.close(); }
				};
			}
		};
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var sources = new MappingSources().addXmlMappingSource( XmlMappingSource.fromInputStreamAccess( access ) );
			assertThat( prepare( sources, registry, false ).xmlMappings() ).isEmpty();
			assertThat( opened.get() ).isZero();
			assertThatThrownBy( () -> prepare( sources, registry, true ) ).isInstanceOf( org.hibernate.boot.InvalidMappingException.class );
			assertThat( opened.get() ).isEqualTo( 1 );
			assertThat( closed.get() ).isEqualTo( 1 );
			final var borrowed = access.accessInputStream();
			assertThatThrownBy( () -> prepare( new MappingSources().addXmlMappingSource( XmlMappingSource.fromInputStream( borrowed ) ),
					registry, true ) ).isInstanceOf( org.hibernate.boot.InvalidMappingException.class );
			assertThat( closed.get() ).isEqualTo( 1 );
		}
	}

	@Test
	void cacheableFilesRetainTheirIdentityAndStrictness() throws Exception {
		final var file = directory.resolve( "cached.xml" );
		Files.writeString( file, mapping( "Cached" ) );
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var strict = new MappingSources().addXmlMappingSource( XmlMappingSource.fromCacheableFile( file.toFile(), null, true ) );
			assertThat( prepare( strict, registry, false ).xmlMappings() ).isEmpty();
			assertThatThrownBy( () -> prepare( strict, registry, true ) ).hasMessageContaining( "Unable to locate cached file" );
			final var sources = new MappingSources().addXmlMappingSource( XmlMappingSource.fromCacheableFile( file.toFile(), null, false ) )
					.addXmlMappingSource( XmlMappingSource.discoveredUri( file.toUri() ) );
			assertThat( prepare( sources, registry, true ).xmlMappings() ).hasSize( 1 );
			assertThat( prepare( strict, registry, true ).xmlMappings() ).hasSize( 1 );
		}
	}

	@Test
	void disabledXmlDoesNotInspectAdmittedArchives() throws Exception {
		final var factory = new org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory() {
			@Override public org.hibernate.boot.archive.spi.ArchiveDescriptor buildArchiveDescriptor(URL url) {
				throw new AssertionError( "Disabled XML must not inspect archives for default mappings" );
			}
		};
		final var root = directory.toUri().toURL();
		final var descriptor = new PersistenceUnitInfoDescriptor( new PersistenceUnitInfoAdapter() {
			@Override public URL getPersistenceUnitRootUrl() { return root; }
		} );
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var scannerCreated = new AtomicInteger();
			final org.hibernate.boot.scan.spi.ScanningProvider provider = scanningContext -> {
				scannerCreated.incrementAndGet();
				assertThat( scanningContext.getProperties() ).containsEntry( MappingSettings.XML_MAPPING_ENABLED, false );
				return new org.hibernate.boot.scan.internal.StandardScanner();
			};
			final var settings = SettingsResolver.resolveBootstrapSettings( Map.of(
					PersistenceSettings.SCANNER_ARCHIVE_INTERPRETER, factory, PersistenceSettings.SCANNING, provider ) );
			final var disabled = SettingsResolver.resolveMappingSettings( SettingsResolver.resolveBootstrapSettings(
					Map.of( MappingSettings.XML_MAPPING_ENABLED, false ) ), FetchType.EAGER );
			for (var adapter : List.of( PersistenceUnitSources.container( descriptor ), PersistenceUnitSources.standalone( descriptor ) )) {
				final var sources = adapter.collect( settings, disabled,
						new ContributionDiscoveryContext( registry.requireService( ClassLoaderService.class ) ) );
				assertThat( prepare( sources, registry, false ).xmlMappings() ).isEmpty();
			}
			assertThat( scannerCreated.get() ).isEqualTo( 1 );
		}
	}

	private static PreparedMappingSources prepare(MappingSources sources, StandardServiceRegistry registry, boolean xmlEnabled) {
		final var context = new MetadataBuildingContextTestingImpl( registry );
		return PreparedMappingSources.from( sources, new MappingSourcePreparationContext( context.getModelsContext(), registry ),
				SettingsResolver.resolveMappingSettings( SettingsResolver.resolveBootstrapSettings(
						Map.of( MappingSettings.XML_MAPPING_ENABLED, xmlEnabled ) ), FetchType.EAGER ) );
	}

	private URL jar(String name, Map<String, String> entries) throws IOException {
		final var path = directory.resolve( name );
		try (var output = new JarOutputStream( Files.newOutputStream( path ) )) {
			for (var entry : entries.entrySet()) {
				output.putNextEntry( new JarEntry( entry.getKey() ) );
				output.write( entry.getValue().getBytes( StandardCharsets.UTF_8 ) );
				output.closeEntry();
			}
		}
		return path.toUri().toURL();
	}

	private static String mapping(String type) {
		return """
				<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2">
				<entity class="example.%s"/>
				</entity-mappings>
				""".formatted( type );
	}
}
