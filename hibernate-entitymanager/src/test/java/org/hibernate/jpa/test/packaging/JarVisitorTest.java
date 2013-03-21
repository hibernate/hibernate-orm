/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.packaging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.archive.internal.ArchiveHelper;
import org.hibernate.jpa.boot.archive.internal.ExplodedArchiveDescriptor;
import org.hibernate.jpa.boot.archive.internal.JarFileBasedArchiveDescriptor;
import org.hibernate.jpa.boot.archive.internal.JarInputStreamBasedArchiveDescriptor;
import org.hibernate.jpa.boot.archive.internal.JarProtocolArchiveDescriptor;
import org.hibernate.jpa.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.internal.ClassDescriptorImpl;
import org.hibernate.jpa.boot.scan.internal.StandardScanOptions;
import org.hibernate.jpa.boot.scan.spi.AbstractScannerImpl;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.pack.defaultpar.Version;
import org.hibernate.jpa.test.pack.explodedpar.Carpet;

import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
@RequiresDialect( H2Dialect.class ) // Nothing dialect-specific -- no need to run in matrix.
@SuppressWarnings("unchecked")
public class JarVisitorTest extends PackagingTestCase {
	@Test
	public void testHttp() throws Exception {
		URL url = ArchiveHelper.getJarURLFromURLEntry(
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
		ArchiveDescriptor archiveDescriptor = StandardArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( url );
		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);
		assertEquals( 0, resultCollector.getClassDescriptorSet().size() );
		assertEquals( 0, resultCollector.getPackageDescriptorSet().size() );
		assertEquals( 0, resultCollector.getMappingFileSet().size() );
	}

	@Test
	public void testInputStreamZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		ArchiveDescriptor archiveDescriptor = new JarInputStreamBasedArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				defaultPar.toURL(),
				""
		);

		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		validateResults( resultCollector, org.hibernate.jpa.test.pack.defaultpar.ApplicationServer.class, Version.class );
	}

	private void validateResults(AbstractScannerImpl.ResultCollector resultCollector, Class... expectedClasses) throws IOException {
		assertEquals( 3, resultCollector.getClassDescriptorSet().size() );
		for ( Class expectedClass : expectedClasses ) {
			assertTrue(
					resultCollector.getClassDescriptorSet().contains(
							new ClassDescriptorImpl( expectedClass.getName(), null )
					)
			);
		}

		assertEquals( 2, resultCollector.getMappingFileSet().size() );
		for ( MappingFileDescriptor mappingFileDescriptor : resultCollector.getMappingFileSet() ) {
			assertNotNull( mappingFileDescriptor.getStreamAccess() );
			final InputStream stream = mappingFileDescriptor.getStreamAccess().accessInputStream();
			assertNotNull( stream );
			stream.close();
		}
	}

	@Test
	public void testNestedJarProtocol() throws Exception {
		File defaultPar = buildDefaultPar();
		File nestedEar = buildNestedEar( defaultPar );
		File nestedEarDir = buildNestedEarDir( defaultPar );
		addPackageToClasspath( nestedEar );

		String jarFileName = nestedEar.toURL().toExternalForm() + "!/defaultpar.par";

		JarProtocolArchiveDescriptor archiveDescriptor = new JarProtocolArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				new URL( jarFileName ),
				""
		);
		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		validateResults( resultCollector, org.hibernate.jpa.test.pack.defaultpar.ApplicationServer.class, Version.class );

		jarFileName = nestedEarDir.toURL().toExternalForm() + "!/defaultpar.par";
		archiveDescriptor = new JarProtocolArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				new URL( jarFileName ),
				""
		);
		resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		validateResults( resultCollector, org.hibernate.jpa.test.pack.defaultpar.ApplicationServer.class, Version.class );
	}

	@Test
	public void testJarProtocol() throws Exception {
		File war = buildWar();
		addPackageToClasspath( war );

		String jarFileName = war.toURL().toExternalForm() + "!/WEB-INF/classes";
		JarProtocolArchiveDescriptor archiveDescriptor = new JarProtocolArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				new URL( jarFileName ),
				""
		);

		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		validateResults(
				resultCollector,
				org.hibernate.jpa.test.pack.war.ApplicationServer.class,
				org.hibernate.jpa.test.pack.war.Version.class
		);
	}

	@Test
	public void testZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		JarFileBasedArchiveDescriptor archiveDescriptor = new JarFileBasedArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				defaultPar.toURL(),
				""
		);
		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		validateResults( resultCollector, org.hibernate.jpa.test.pack.defaultpar.ApplicationServer.class, Version.class );
	}

	@Test
	public void testExplodedJar() throws Exception {
		File explodedPar = buildExplodedPar();
		addPackageToClasspath( explodedPar );

		String dirPath = explodedPar.toURL().toExternalForm();
		// TODO - shouldn't  ExplodedJarVisitor take care of a trailing slash?
		if ( dirPath.endsWith( "/" ) ) {
			dirPath = dirPath.substring( 0, dirPath.length() - 1 );
		}

		ExplodedArchiveDescriptor archiveDescriptor = new ExplodedArchiveDescriptor(
				StandardArchiveDescriptorFactory.INSTANCE,
				ArchiveHelper.getURLFromPath( dirPath ),
				""
		);
		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		assertEquals( 1, resultCollector.getClassDescriptorSet().size() );
		assertEquals( 1, resultCollector.getPackageDescriptorSet().size() );
		assertEquals( 1, resultCollector.getMappingFileSet().size() );

		assertTrue(
				resultCollector.getClassDescriptorSet().contains(
						new ClassDescriptorImpl( Carpet.class.getName(), null )
				)
		);

		for ( MappingFileDescriptor mappingFileDescriptor : resultCollector.getMappingFileSet() ) {
			assertNotNull( mappingFileDescriptor.getStreamAccess() );
			final InputStream stream = mappingFileDescriptor.getStreamAccess().accessInputStream();
			assertNotNull( stream );
			stream.close();
		}
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-6806")
	public void testJarVisitorFactory() throws Exception {
		final File explodedPar = buildExplodedPar();
		final File defaultPar = buildDefaultPar();
		addPackageToClasspath( explodedPar, defaultPar );
		
        //setting URL to accept vfs based protocol
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
			public URLStreamHandler createURLStreamHandler(String protocol) {
				if("vfszip".equals(protocol) || "vfsfile".equals(protocol) )
				return new URLStreamHandler() {
					protected URLConnection openConnection(URL u)
							throws IOException {
						return null;
					}
				};
				return null;
			}
		});
        
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
	@TestForIssue( jiraKey = "EJB-230" )
	public void testDuplicateFilterExplodedJarExpected() throws Exception {
//		File explodedPar = buildExplodedPar();
//		addPackageToClasspath( explodedPar );
//
//		Filter[] filters = getFilters();
//		Filter[] dupeFilters = new Filter[filters.length * 2];
//		int index = 0;
//		for ( Filter filter : filters ) {
//			dupeFilters[index++] = filter;
//		}
//		filters = getFilters();
//		for ( Filter filter : filters ) {
//			dupeFilters[index++] = filter;
//		}
//		String dirPath = explodedPar.toURL().toExternalForm();
//		// TODO - shouldn't  ExplodedJarVisitor take care of a trailing slash?
//		if ( dirPath.endsWith( "/" ) ) {
//			dirPath = dirPath.substring( 0, dirPath.length() - 1 );
//		}
//		JarVisitor jarVisitor = new ExplodedJarVisitor( dirPath, dupeFilters );
//		assertEquals( "explodedpar", jarVisitor.getUnqualifiedJarName() );
//		Set[] entries = jarVisitor.getMatchingEntries();
//		assertEquals( 1, entries[1].size() );
//		assertEquals( 1, entries[0].size() );
//		assertEquals( 1, entries[2].size() );
//		for ( Entry entry : ( Set<Entry> ) entries[2] ) {
//			InputStream is = entry.getInputStream();
//			if ( is != null ) {
//				assertTrue( 0 < is.available() );
//				is.close();
//			}
//		}
//		for ( Entry entry : ( Set<Entry> ) entries[5] ) {
//			InputStream is = entry.getInputStream();
//			if ( is != null ) {
//				assertTrue( 0 < is.available() );
//				is.close();
//			}
//		}
//
//		Entry entry = new Entry( Carpet.class.getName(), null );
//		assertTrue( entries[1].contains( entry ) );
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-7835")
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
		assertTrue( oldTime > newTime );
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
	@TestForIssue(jiraKey = "HHH-7835")
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
