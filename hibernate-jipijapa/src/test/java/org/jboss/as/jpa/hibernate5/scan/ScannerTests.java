/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.jboss.as.jpa.hibernate5.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.vfs.TempFileProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ScannerTests {
	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
	protected static ClassLoader bundleClassLoader;

	protected static TempFileProvider tempFileProvider;

	protected static File testSrcDirectory;

	/**
	 * Directory where shrink-wrap built archives are written
	 */
	protected static File shrinkwrapArchiveDirectory;

	static {
		try {
			tempFileProvider = TempFileProvider.create( "test", new ScheduledThreadPoolExecutor( 2 ) );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		// we make an assumption here that the directory which holds compiled classes (nested) also holds
		// sources.   We therefore look for our module directory name, and use that to locate bundles
		final URL scannerTestsClassFileUrl = originalClassLoader.getResource(
				ScannerTests.class.getName().replace( '.', '/' ) + ".class"
		);
		if ( scannerTestsClassFileUrl == null ) {
			// blow up
			fail( "Could not find ScannerTests class file url" );
		}

		// look for the module name in that url
		final int position = scannerTestsClassFileUrl.getFile().lastIndexOf( "/hibernate5/" );

		if ( position == -1 ) {
			fail( "Unable to setup packaging test" );
		}

		final String moduleDirectoryPath = scannerTestsClassFileUrl.getFile().substring(
				0,
				position + "/hibernate5".length()
		);
		final File moduleDirectory = new File( moduleDirectoryPath );

		testSrcDirectory = new File( new File( moduleDirectory, "src" ), "test" );
		final File bundlesDirectory = new File( testSrcDirectory, "bundles" );
		try {
			bundleClassLoader = new URLClassLoader( new URL[] {bundlesDirectory.toURL()}, originalClassLoader );
		}
		catch (MalformedURLException e) {
			fail( "Unable to build custom class loader" );
		}

		shrinkwrapArchiveDirectory = new File( moduleDirectory, "target/packages" );
		shrinkwrapArchiveDirectory.mkdirs();
	}

	@Before
	public void prepareTCCL() {
		// add the bundle class loader in order for ShrinkWrap to build the test package
		Thread.currentThread().setContextClassLoader( bundleClassLoader );
	}

	@After
	public void resetTCCL() throws Exception {
		// reset the classloader
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}

	protected File buildLargeJar() throws Exception {
		final String fileName = "large.jar";
		final JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		// Build a large jar by adding a lorem ipsum file repeatedly.
		final Path loremipsumTxtFile = Paths.get( ScannerTests.class.getResource(
				"/org/hibernate/jpa/test/packaging/loremipsum.txt" ).toURI() );
		for ( int i = 0; i < 100; i++ ) {
			ArchivePath path = ArchivePaths.create( "META-INF/file" + i );
			archive.addAsResource( loremipsumTxtFile.toFile(), path );
		}

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	@Test
	public void testGetBytesFromInputStream() throws Exception {
		File file = buildLargeJar();

		InputStream stream = new BufferedInputStream(
				new FileInputStream( file ) );
		int oldLength = getBytesFromInputStream( stream ).length;
		stream.close();

		stream = new BufferedInputStream( new FileInputStream( file ) );
		int newLength = ArchiveHelper.getBytesFromInputStream( stream ).length;
		stream.close();

		assertEquals( oldLength, newLength );
	}

	// This is the old getBytesFromInputStream from JarVisitorFactory before
	// it was changed by HHH-7835. Use it as a regression test.
	private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		int size;

		byte[] entryBytes = new byte[0];
		for ( ; ; ) {
			byte[] tmpByte = new byte[4096];
			size = inputStream.read( tmpByte );
			if ( size == -1 ) {
				break;
			}
			byte[] current = new byte[entryBytes.length + size];
			System.arraycopy( entryBytes, 0, current, 0, entryBytes.length );
			System.arraycopy( tmpByte, 0, current, entryBytes.length, size );
			entryBytes = current;
		}
		return entryBytes;
	}

	@Test
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
