/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;

import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.Environment;
import org.hibernate.scan.jandex.ScanningProviderImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/// @author Steve Ebersole
public class SimpleScanningTests {
	private static final DotName JAKARTA_DATA_REPOSITORY =
			DotName.createSimple( "jakarta.data.repository.Repository" );

	@Test
	@ServiceRegistry
	void testSimpleJarScanning(@TempDir File stagingDir, ServiceRegistryScope registryScope) throws IOException {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// the deployment
		var deployment = buildJar( "my-model.jar", stagingDir );
		var rootUrl = deployment.toURI().toURL();

		var jandexIndex = buildJandexIndex();

		var properties = new HashMap<>( Environment.getProperties() );
		properties.put( ScanningProviderImpl.JANDEX_INDEX, jandexIndex );
		var scanningContext = new ScanningContextImpl(
				new StandardArchiveDescriptorFactory(),
				properties
		);

		var scannerProvider = new ScanningProviderImpl();
		var scanner = scannerProvider.builderScanner( scanningContext );

		final ScanningResult scanResult = scanner.scan( rootUrl );
		assertDiscoveredClasses( scanResult );
	}

	@Test
	void selectedArchivesBoundProvidedIndex(@TempDir File stagingDir) throws IOException {
		final var archive = ShrinkWrap.create( JavaArchive.class, "selected.jar" )
				.addClasses( Book.class, Entity.class )
				.addAsManifestResource( new org.jboss.shrinkwrap.api.asset.StringAsset( "deliberately not XML" ), "orm.xml" );
		final var file = new File( stagingDir, "selected.jar" );
		archive.as( ZipExporter.class ).exportTo( file, true );
		for ( var settings : List.of( Map.<String, Object>of(), Map.<String, Object>of( ScanningProviderImpl.JANDEX_INDEX, buildJandexIndex() ) ) ) {
			final var context = new ScanningContextImpl( new StandardArchiveDescriptorFactory(), settings );
			final var scanner = new ScanningProviderImpl().builderScanner( context );
			org.assertj.core.api.Assertions.assertThatThrownBy( () -> scanner.scan( new File( stagingDir, "missing.jar" ).toURI().toURL() ) )
					.isInstanceOf( ArchiveException.class );
			final var result = scanner.scan( file.toURI().toURL() );
			assertThat( result.discoveredClasses() ).containsExactly( Book.class.getName() );
			assertThat( result.mappingFiles() ).hasSize( 1 );
			org.assertj.core.api.Assertions.assertThatThrownBy( () -> result.discoveredClasses().clear() )
					.isInstanceOf( UnsupportedOperationException.class );
			final var unit = new JaxbPersistenceImpl.JaxbPersistenceUnitImpl();
			final var root = context.getArchiveDescriptorFactory().buildArchiveDescriptor( file.toURI().toURL() );
			for ( Boolean exclude : new Boolean[] { null, true, false } ) {
				unit.setExcludeUnlistedClasses( exclude );
				final var jpaResult = scanner.jpaScan( root, unit );
				assertThat( jpaResult.discoveredClasses() ).hasSize( Boolean.FALSE.equals( exclude ) ? 1 : 0 );
				assertThat( jpaResult.mappingFiles() ).hasSize( 1 );
			}
		}
	}

	private void assertDiscoveredClasses(ScanningResult scanResult) {
		assertThat( scanResult.discoveredClasses() )
				.contains(
						Book.class.getName(),
						FirstClass.class.getName(),
						BookRepository.class.getName()
				)
				.doesNotContain( SecondClass.class.getName() );
	}

	private IndexView buildJandexIndex() {
		try {
			var indexer = new Indexer();
			indexer.indexClass( Book.class );
			indexer.indexClass( BookRepository.class );
			indexer.indexClass( FirstClass.class );
			indexer.indexClass( SecondClass.class );
			indexer.indexClass( Entity.class );
			indexer.indexClass( SuperCoolFeature.class );
			return indexer.complete();
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to build Jandex index", e );
		}
	}

	private File buildJar(String fileName, File stagingDir) {
		var jarArchive = ShrinkWrap.create( JavaArchive.class, fileName );
		jarArchive.addClasses( Book.class, BookRepository.class, FirstClass.class, SecondClass.class, Entity.class, SuperCoolFeature.class );
		var exportedArchive = new File( stagingDir, fileName );
		jarArchive.as( ZipExporter.class ).exportTo( exportedArchive, true );
		return exportedArchive;
	}
}
