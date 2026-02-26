/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.scan.Discoverable;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.Environment;
import org.hibernate.scan.jandex.ScanningProviderImpl;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
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

import static org.assertj.core.api.Assertions.assertThat;

/// @author Steve Ebersole
public class SimpleScanningTests {
	@Test
	@ServiceRegistry
	@NotImplementedYet(reason = "Needs the next release of JPA 4.0 with @Discoverable")
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
		assertThat( scanResult.discoveredClasses() ).hasSize( 2 );
	}

	private IndexView buildJandexIndex() {
		try {
			var indexer = new Indexer();
			indexer.indexClass( Book.class );
			indexer.indexClass( FirstClass.class );
			indexer.indexClass( SecondClass.class );
			indexer.indexClass( Discoverable.class );
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
		jarArchive.addClasses( Book.class, FirstClass.class, SecondClass.class );
		var exportedArchive = new File( stagingDir, fileName );
		jarArchive.as( ZipExporter.class ).exportTo( exportedArchive, true );
		return exportedArchive;
	}
}
