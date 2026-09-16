/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;

import jakarta.persistence.spi.Discoverable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.scan.jandex.ScanningProviderImpl;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/// Real module and package classfiles exercise both scanner paths and archive boundaries.
///
/// @author Steve Ebersole
class DescriptorScanningTests {
	@Test
	void descriptorsRespectBoundariesAndDiscoverability(@TempDir Path directory) throws Exception {
		final var indexer = new Indexer();
		final var listed = module( directory, "listed", true, indexer );
		final var root = module( directory, "root", true, indexer );
		module( directory, "unrelated", true, indexer );
		final var plain = module( directory, "plain", false, indexer );
		final var listedFile = directory.resolve( "listed.jar" ).toFile();
		listed.as( ZipExporter.class ).exportTo( listedFile, true );
		root.addAsResource( listedFile, "listed.jar" );
		final var rootFile = directory.resolve( "root.jar" ).toFile();
		root.as( ZipExporter.class ).exportTo( rootFile, true );
		final var plainFile = directory.resolve( "plain.jar" ).toFile();
		plain.as( ZipExporter.class ).exportTo( plainFile, true );
		final var supplied = indexer.complete();
		for ( var settings : List.of( Map.<String, Object>of(), Map.<String, Object>of( ScanningProviderImpl.JANDEX_INDEX, supplied ) ) ) {
			final var context = new ScanningContextImpl( new StandardArchiveDescriptorFactory(), settings );
			final var scanner = new ScanningProviderImpl().builderScanner( context );
			final var result = scanner.scan( rootFile.toURI().toURL(), listedFile.toURI().toURL(), plainFile.toURI().toURL() );
			assertThat( result.discoveredModules() ).containsExactlyInAnyOrder( "fixture.root", "fixture.listed" );
			assertThat( result.discoveredPackages() ).containsExactlyInAnyOrder( "fixture.root", "fixture.listed" );
			assertThat( result.discoveredClasses() ).isEmpty();
			final var unit = new JaxbPersistenceUnitImpl();
			unit.getJarFiles().add( "listed.jar" );
			final var archive = context.getArchiveDescriptorFactory().buildArchiveDescriptor( rootFile.toURI().toURL() );
			for ( Boolean exclude : new Boolean[] { null, true, false } ) {
				unit.setExcludeUnlistedClasses( exclude );
				final var jpaResult = scanner.jpaScan( archive, unit );
				assertThat( jpaResult.discoveredModules() ).contains( "fixture.listed" ).doesNotContain( "fixture.unrelated", "fixture.plain" );
				assertThat( jpaResult.discoveredPackages() ).contains( "fixture.listed" ).doesNotContain( "fixture.unrelated", "fixture.plain" );
				assertThat( jpaResult.discoveredModules().contains( "fixture.root" ) ).isEqualTo( Boolean.FALSE.equals( exclude ) );
				assertThat( jpaResult.discoveredPackages().contains( "fixture.root" ) ).isEqualTo( Boolean.FALSE.equals( exclude ) );
				assertThat( jpaResult.discoveredClasses() ).isEmpty();
			}
		}
	}

	private JavaArchive module(Path directory, String suffix, boolean discoverable, Indexer indexer) throws Exception {
		final var source = Files.createDirectories( directory.resolve( suffix ).resolve( "src" ) );
		final var output = Files.createDirectories( directory.resolve( suffix ).resolve( "classes" ) );
		final var packageName = "fixture." + suffix;
		final var descriptor = source.resolve( "module-info.java" );
		Files.writeString( descriptor, "/// @author Steve Ebersole\n" + ( discoverable ? "@" + packageName + ".Marker\n" : "" )
				+ "module " + packageName + " { requires jakarta.persistence; }" );
		final var marker = source.resolve( "Marker.java" );
		Files.writeString( marker, """
				package %s;
				import jakarta.persistence.spi.Discoverable;
				/// @author Steve Ebersole
				@Discoverable
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
				@java.lang.annotation.Target({java.lang.annotation.ElementType.MODULE, java.lang.annotation.ElementType.PACKAGE})
				public @interface Marker {}
				""".formatted( packageName ) );
		final var packageInfo = source.resolve( "package-info.java" );
		Files.writeString( packageInfo, "/// @author Steve Ebersole\n" + ( discoverable ? "@" + packageName + ".Marker\n" : "" ) + "package " + packageName + ";" );
		assertThat( ToolProvider.getSystemJavaCompiler().run( null, null, null,
				"--module-path", Path.of( Discoverable.class.getProtectionDomain().getCodeSource().getLocation().toURI() ).toString(),
				"-d", output.toString(), descriptor.toString(), marker.toString(), packageInfo.toString() ) ).isZero();
		final var archive = ShrinkWrap.create( JavaArchive.class, suffix + ".jar" );
		try ( var files = Files.walk( output ) ) {
			for ( var file : files.filter( path -> path.toString().endsWith( ".class" ) ).toList() ) {
				archive.addAsResource( file.toFile(), output.relativize( file ).toString() );
				try ( var stream = Files.newInputStream( file ) ) {
					indexer.index( stream );
				}
			}
		}
		return archive;
	}
}
