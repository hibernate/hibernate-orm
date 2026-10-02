package org.hibernate.test.scan.jandex;

import jakarta.persistence.spi.Discoverable;

import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.scan.jandex.ScanningProviderImpl;
import org.hibernate.testing.orm.module.TestModule;
import org.hibernate.test.scan.jandex.fixture.DescriptorMarker;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/// Real module and package classfiles exercise both scanner paths and archive boundaries.
///
/// @author Steve Ebersole
class DescriptorScanningTests {

	// Ensure no JAR files are being cached to avoid file deletion issues on Windows
	private static boolean jarDefaultUseCaches;

	@BeforeAll
	public static void setup() {
		jarDefaultUseCaches = URLConnection.getDefaultUseCaches( "jar" );
		URLConnection.setDefaultUseCaches( "jar", false );
	}

	@AfterAll
	public static void cleanup() {
		URLConnection.setDefaultUseCaches( "jar", jarDefaultUseCaches );
	}

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
		final var packageName = "fixture." + suffix;
		final var archive = ShrinkWrap.create( JavaArchive.class, suffix + ".jar" )
				.addClass( DescriptorMarker.class );
		if ( discoverable ) {
			archive.addClass( Class.forName( packageName + ".package-info" ) );
		}
		final var declaration = ( discoverable ? "@" + DescriptorMarker.class.getName() + "\n" : "" )
				+ "module " + packageName + " {}";
		try (var module = TestModule.load( archive, declaration, directory, DescriptorMarker.class, Discoverable.class )) {
			try ( var stream = module.module().getResourceAsStream( "module-info.class" ) ) {
				assertThat( stream ).isNotNull();
				archive.add( new ByteArrayAsset( stream.readAllBytes() ), "module-info.class" );
			}
		}
		for ( var entry : archive.getContent().entrySet() ) {
			if ( entry.getKey().get().endsWith( ".class" ) ) {
				try ( var stream = entry.getValue().getAsset().openStream() ) {
					indexer.index( stream );
				}
			}
		}
		return archive;
	}
}
