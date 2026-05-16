/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.Environment;
import org.hibernate.scan.jandex.ScanningProviderImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
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
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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
			return addRepositoryAnnotation( indexer.complete() );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to build Jandex index", e );
		}
	}

	private IndexView addRepositoryAnnotation(Index index) {
		final ClassInfo repositoryClass =
				index.getClassByName( DotName.createSimple( BookRepository.class.getName() ) );
		return Index.create(
				Map.of(
						DotName.createSimple( Discoverable.class.getName() ),
						List.copyOf( index.getAnnotations( Discoverable.class ) ),
						DotName.createSimple( Entity.class.getName() ),
						List.copyOf( index.getAnnotations( Entity.class ) ),
						DotName.createSimple( SuperCoolFeature.class.getName() ),
						List.copyOf( index.getAnnotations( SuperCoolFeature.class ) ),
						JAKARTA_DATA_REPOSITORY,
						List.of( AnnotationInstance.create( JAKARTA_DATA_REPOSITORY, repositoryClass, List.of() ) )
				),
				Map.of(),
				Map.of(),
				index.getKnownClasses().stream().collect( toMap( ClassInfo::name, identity() ) )
		);
	}

	private File buildJar(String fileName, File stagingDir) {
		var jarArchive = ShrinkWrap.create( JavaArchive.class, fileName );
		jarArchive.addClasses( Book.class, BookRepository.class, FirstClass.class, SecondClass.class );
		var exportedArchive = new File( stagingDir, fileName );
		jarArchive.as( ZipExporter.class ).exportTo( exportedArchive, true );
		return exportedArchive;
	}
}
