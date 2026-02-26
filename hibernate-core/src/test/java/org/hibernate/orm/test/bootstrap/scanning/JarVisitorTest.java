/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.archive.internal.ExplodedArchiveDescriptor;
import org.hibernate.boot.archive.internal.JarFileBasedArchiveDescriptor;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.pack.defaultpar.Version;
import org.hibernate.orm.test.jpa.pack.explodedpar.Carpet;
import org.hibernate.scan.jandex.ProvidedIndexScanner;
import org.hibernate.scan.jandex.IndexBuildingScanner;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
@RequiresDialect( H2Dialect.class ) // Nothing dialect-specific -- no need to run in matrix.
@ServiceRegistry
public class JarVisitorTest extends PackagingTestCase {
	@Test
	public void testHttp(ServiceRegistryScope registryScope) throws Exception {
		final URL url = ArchiveHelper.getJarURLFromURLEntry(
				new URL(
						"jar:http://www.ibiblio.org/maven/hibernate/jars/hibernate-annotations-3.0beta1.jar!/META-INF/persistence.xml"
				),
				"/META-INF/persistence.xml"
		);
		try {
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
		}
		catch ( IOException ie ) {
			//fail silently
			return;
		}

		ScanningResult result = standardScan( url, null, registryScope.getRegistry() );
		assertEquals( 0, result.discoveredClasses().size() );
		assertEquals( 0, result.discoveredPackages().size() );
	}

	private ScanningResult standardScan(URL url, IndexView jandexIndex, StandardServiceRegistry registry) {
		var context = new ScanningContextImpl(
				new StandardArchiveDescriptorFactory(),
				Environment.getProperties()
		);
		final Scanner scanner;
		if ( jandexIndex == null ) {
			scanner = new IndexBuildingScanner( context );
		}
		else {
			scanner = new ProvidedIndexScanner( context, jandexIndex );
		}
		return scanner.scan( url );
	}

	@Test
	public void testInputStreamZippedJar(ServiceRegistryScope registryScope) throws Exception {
		var jandexIndex = buildDefaultParIndex();
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		var result = standardScan( defaultPar.toURL(), jandexIndex, registryScope.getRegistry() );
		validateResults( result, org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer.class, Version.class );
	}

	private void validateResults(ScanningResult scanResult, Class... expectedClasses) throws IOException {
		assertEquals( 3, scanResult.discoveredClasses().size() );
		for ( Class<?> expectedClass : expectedClasses ) {
			assertTrue( scanResult.discoveredClasses().contains( expectedClass.getName() ) );
		}

	}

	@Test
	public void testNestedJarProtocol(ServiceRegistryScope registryScope) throws Exception {
		var jandexIndex = buildDefaultParIndex();
		File defaultPar = buildDefaultPar();
		File nestedEar = buildNestedEar( defaultPar );
		File nestedEarDir = buildNestedEarDir( defaultPar );
		addPackageToClasspath( nestedEar );

		String jarFileName = nestedEar.toURL().toExternalForm() + "!/defaultpar.par";
		URL rootUrl = new URL( jarFileName );

		var results = standardScan( rootUrl, jandexIndex, registryScope.getRegistry() );
		validateResults(
				results,
				org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer.class,
				Version.class
		);

		jarFileName = nestedEarDir.toURL().toExternalForm() + "!/defaultpar.par";
		rootUrl = new URL( jarFileName );

		results = standardScan( rootUrl, jandexIndex, registryScope.getRegistry() );
		validateResults(
				results,
				org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer.class,
				Version.class
		);
	}

	@Test
	public void testJarProtocol(ServiceRegistryScope registryScope) throws Exception {
		IndexView jandexIndex = buildWarIndex();
		File war = buildWar();
		addPackageToClasspath( war );

		String jarFileName = war.toURL().toExternalForm() + "!/WEB-INF/classes";
		URL rootUrl = new URL( jarFileName );

		var results = standardScan( rootUrl, jandexIndex, registryScope.getRegistry() );
		validateResults(
				results,
				org.hibernate.orm.test.jpa.pack.war.ApplicationServer.class,
				org.hibernate.orm.test.jpa.pack.war.Version.class
		);
	}

	@Test
	public void testZippedJar(ServiceRegistryScope registryScope) throws Exception {
		var jandexIndex = buildDefaultParIndex();
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		var results = standardScan( defaultPar.toURL(), jandexIndex, registryScope.getRegistry() );
		validateResults(
				results,
				org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer.class,
				Version.class
		);
	}

	@Test
	public void testExplodedJar(ServiceRegistryScope registryScope) throws Exception {
		var jandexIndex = buildExplodedParIndex();
		File explodedPar = buildExplodedPar();
		addPackageToClasspath( explodedPar );

		var result = standardScan( explodedPar.toURL(), jandexIndex, registryScope.getRegistry() );
		assertEquals( 1, result.discoveredClasses().size() );
		assertEquals( 1, result.discoveredPackages().size() );

		assertTrue( result.discoveredClasses().contains( Carpet.class.getName() ) );
	}

	@Test
	@JiraKey(value = "HHH-6806")
	public void testJarVisitorFactory(ServiceRegistryScope registryScope) throws Exception {
		final File explodedPar = buildExplodedPar();
		final File defaultPar = buildDefaultPar();
		addPackageToClasspath( explodedPar, defaultPar );

		//setting URL to accept vfs based protocol
		URL.setURLStreamHandlerFactory( protocol -> {
			if("vfszip".equals(protocol) || "vfsfile".equals(protocol) )
				return new URLStreamHandler() {
					protected URLConnection openConnection(URL u)
							throws IOException {
						return null;
					}
				};
			return null;
		} );

		URL jarUrl = defaultPar.toURL();
		ArchiveDescriptor descriptor = StandardArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( jarUrl );
		assertEquals( JarFileBasedArchiveDescriptor.class.getName(), descriptor.getClass().getName() );

		jarUrl  = explodedPar.toURL();
		descriptor = StandardArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( jarUrl );
		assertEquals( ExplodedArchiveDescriptor.class.getName(), descriptor.getClass().getName() );

		jarUrl  = new URL( defaultPar.toURL().toExternalForm().replace( "file:", "vfszip:" ) );
		descriptor = StandardArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( jarUrl );
		assertEquals( JarFileBasedArchiveDescriptor.class.getName(), descriptor.getClass().getName());

		jarUrl  = new URL( explodedPar.toURL().toExternalForm().replace( "file:", "vfsfile:" ) );
		descriptor = StandardArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( jarUrl );
		assertEquals( ExplodedArchiveDescriptor.class.getName(), descriptor.getClass().getName() );
	}

	@Test
	@JiraKey(value = "HHH-7835")
	public void testGetBytesFromInputStream() throws Exception {
		File file = buildLargeJar();

		long start = System.currentTimeMillis();
		InputStream stream = new BufferedInputStream(
				new FileInputStream( file ) );
		int oldLength = getBytesFromInputStream( stream ).length;
		stream.close();
		long oldTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		stream = new BufferedInputStream( new FileInputStream( file ) );
		int newLength = ArchiveHelper.getBytesFromInputStream( stream ).length;
		stream.close();
		long newTime = System.currentTimeMillis() - start;

		assertEquals( oldLength, newLength );

		System.out.printf(
				"InputStream byte[] extraction algorithms; old = `%s`, new = `%s`",
				oldTime,
				newTime
		);
	}

	// This is the old getBytesFromInputStream from JarVisitorFactory before
	// it was changed by HHH-7835. Use it as a regression test.
	private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		int size;

		byte[] entryBytes = new byte[0];
		for ( ;; ) {
			byte[] tmpByte = new byte[4096];
			size = inputStream.read( tmpByte );
			if ( size == -1 )
				break;
			byte[] current = new byte[entryBytes.length + size];
			System.arraycopy( entryBytes, 0, current, 0, entryBytes.length );
			System.arraycopy( tmpByte, 0, current, entryBytes.length, size );
			entryBytes = current;
		}
		return entryBytes;
	}

	@Test
	@JiraKey(value = "HHH-7835")
	public void testGetBytesFromZeroInputStream() throws Exception {
		// Ensure that JarVisitorFactory#getBytesFromInputStream
		// can handle 0 length streams gracefully.
		URL emptyTxtUrl = getClass().getResource( "/org/hibernate/jpa/test/packaging/empty.txt" );
		if ( emptyTxtUrl == null ) {
			throw new RuntimeException( "Bah!" );
		}
		InputStream emptyStream = new BufferedInputStream( emptyTxtUrl.openStream() );
		int length = ArchiveHelper.getBytesFromInputStream( emptyStream ).length;
		assertEquals( length, 0 );
		emptyStream.close();
	}
}
