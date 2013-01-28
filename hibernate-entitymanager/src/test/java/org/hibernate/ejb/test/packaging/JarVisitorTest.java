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
package org.hibernate.ejb.test.packaging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.ejb.packaging.ClassFilter;
import org.hibernate.ejb.packaging.Entry;
import org.hibernate.ejb.packaging.ExplodedJarVisitor;
import org.hibernate.ejb.packaging.FileFilter;
import org.hibernate.ejb.packaging.FileZippedJarVisitor;
import org.hibernate.ejb.packaging.Filter;
import org.hibernate.ejb.packaging.InputStreamZippedJarVisitor;
import org.hibernate.ejb.packaging.JarProtocolVisitor;
import org.hibernate.ejb.packaging.JarVisitor;
import org.hibernate.ejb.packaging.JarVisitorFactory;
import org.hibernate.ejb.packaging.PackageFilter;
import org.hibernate.ejb.test.pack.defaultpar.ApplicationServer;
import org.hibernate.ejb.test.pack.explodedpar.Carpet;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
@SuppressWarnings("unchecked")
@RequiresDialect(H2Dialect.class)
public class JarVisitorTest extends PackagingTestCase {
	@Test
	public void testHttp() throws Exception {
		URL url = JarVisitorFactory.getJarURLFromURLEntry(
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
		JarVisitor visitor = JarVisitorFactory.getVisitor( url, getFilters() );
		assertEquals( 0, visitor.getMatchingEntries()[0].size() );
		assertEquals( 0, visitor.getMatchingEntries()[1].size() );
		assertEquals( 0, visitor.getMatchingEntries()[2].size() );
	}

	@Test
	public void testInputStreamZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		Filter[] filters = getFilters();
		JarVisitor jarVisitor = new InputStreamZippedJarVisitor( defaultPar.toURL(), filters, "" );
		assertEquals( "defaultpar", jarVisitor.getUnqualifiedJarName() );
		Set entries = jarVisitor.getMatchingEntries()[1];
		assertEquals( 3, entries.size() );
		Entry entry = new Entry( ApplicationServer.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		entry = new Entry( org.hibernate.ejb.test.pack.defaultpar.Version.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		assertNull( ( ( Entry ) entries.iterator().next() ).getInputStream() );
		assertEquals( 2, jarVisitor.getMatchingEntries()[2].size() );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}
	}

	@Test
	public void testNestedJarProtocol() throws Exception {
		File defaultPar = buildDefaultPar();
		File nestedEar = buildNestedEar( defaultPar );
		File nestedEarDir = buildNestedEarDir( defaultPar );
		addPackageToClasspath( nestedEar );

		String jarFileName = nestedEar.toURL().toExternalForm() + "!/defaultpar.par";
		Filter[] filters = getFilters();
		JarVisitor jarVisitor = new JarProtocolVisitor( new URL( jarFileName ), filters, "" );
		//TODO should we fix the name here to reach defaultpar rather than nestedjar ??
		//assertEquals( "defaultpar", jarVisitor.getUnqualifiedJarName() );
		Set entries = jarVisitor.getMatchingEntries()[1];
		assertEquals( 3, entries.size() );
		Entry entry = new Entry( ApplicationServer.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		entry = new Entry( org.hibernate.ejb.test.pack.defaultpar.Version.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		assertNull( ( ( Entry ) entries.iterator().next() ).getInputStream() );
		assertEquals( 2, jarVisitor.getMatchingEntries()[2].size() );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}

		jarFileName = nestedEarDir.toURL().toExternalForm() + "!/defaultpar.par";
		//JarVisitor jarVisitor = new ZippedJarVisitor( jarFileName, true, true );
		filters = getFilters();
		jarVisitor = new JarProtocolVisitor( new URL( jarFileName ), filters, "" );
		//TODO should we fix the name here to reach defaultpar rather than nestedjar ??
		//assertEquals( "defaultpar", jarVisitor.getUnqualifiedJarName() );
		entries = jarVisitor.getMatchingEntries()[1];
		assertEquals( 3, entries.size() );
		entry = new Entry( ApplicationServer.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		entry = new Entry( org.hibernate.ejb.test.pack.defaultpar.Version.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		assertNull( ( ( Entry ) entries.iterator().next() ).getInputStream() );
		assertEquals( 2, jarVisitor.getMatchingEntries()[2].size() );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}
	}

	@Test
	public void testJarProtocol() throws Exception {
		File war = buildWar();
		addPackageToClasspath( war );

		String jarFileName = war.toURL().toExternalForm() + "!/WEB-INF/classes";
		Filter[] filters = getFilters();
		JarVisitor jarVisitor = new JarProtocolVisitor( new URL( jarFileName ), filters, "" );
		assertEquals( "war", jarVisitor.getUnqualifiedJarName() );
		Set entries = jarVisitor.getMatchingEntries()[1];
		assertEquals( 3, entries.size() );
		Entry entry = new Entry( org.hibernate.ejb.test.pack.war.ApplicationServer.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		entry = new Entry( org.hibernate.ejb.test.pack.war.Version.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		assertNull( ( ( Entry ) entries.iterator().next() ).getInputStream() );
		assertEquals( 2, jarVisitor.getMatchingEntries()[2].size() );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}
	}

	@Test
	public void testZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		Filter[] filters = getFilters();
		JarVisitor jarVisitor = new FileZippedJarVisitor( defaultPar.toURL(), filters, "" );
		assertEquals( "defaultpar", jarVisitor.getUnqualifiedJarName() );
		Set entries = jarVisitor.getMatchingEntries()[1];
		assertEquals( 3, entries.size() );
		Entry entry = new Entry( ApplicationServer.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		entry = new Entry( org.hibernate.ejb.test.pack.defaultpar.Version.class.getName(), null );
		assertTrue( entries.contains( entry ) );
		assertNull( ( ( Entry ) entries.iterator().next() ).getInputStream() );
		assertEquals( 2, jarVisitor.getMatchingEntries()[2].size() );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}
	}

	@Test
	public void testExplodedJar() throws Exception {
		File explodedPar = buildExplodedPar();
		addPackageToClasspath( explodedPar );

		Filter[] filters = getFilters();
		String dirPath = explodedPar.toURL().toExternalForm();
		// TODO - shouldn't  ExplodedJarVisitor take care of a trailing slash?
		if ( dirPath.endsWith( "/" ) ) {
			dirPath = dirPath.substring( 0, dirPath.length() - 1 );
		}
		JarVisitor jarVisitor = new ExplodedJarVisitor( dirPath, filters );
		assertEquals( "explodedpar", jarVisitor.getUnqualifiedJarName() );
		Set[] entries = jarVisitor.getMatchingEntries();
		assertEquals( 1, entries[1].size() );
		assertEquals( 1, entries[0].size() );
		assertEquals( 1, entries[2].size() );

		Entry entry = new Entry( Carpet.class.getName(), null );
		assertTrue( entries[1].contains( entry ) );
		for ( Entry localEntry : ( Set<Entry> ) jarVisitor.getMatchingEntries()[2] ) {
			assertNotNull( localEntry.getInputStream() );
			localEntry.getInputStream().close();
		}
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-6806")
	public void testJarVisitorFactory() throws Exception{
		
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
        
		URL jarUrl  = new URL ("file:./target/packages/defaultpar.par");
		JarVisitor jarVisitor =  JarVisitorFactory.getVisitor(jarUrl, getFilters(), null);
		assertEquals(FileZippedJarVisitor.class.getName(), jarVisitor.getClass().getName());
		
		jarUrl  = new URL ("file:./target/packages/explodedpar");
		jarVisitor =  JarVisitorFactory.getVisitor(jarUrl, getFilters(), null);
		assertEquals(ExplodedJarVisitor.class.getName(), jarVisitor.getClass().getName());
		
		jarUrl  = new URL ("vfszip:./target/packages/defaultpar.par");
		jarVisitor =  JarVisitorFactory.getVisitor(jarUrl, getFilters(), null);
		assertEquals(FileZippedJarVisitor.class.getName(), jarVisitor.getClass().getName());
		
		jarUrl  = new URL ("vfsfile:./target/packages/explodedpar");
		jarVisitor =  JarVisitorFactory.getVisitor(jarUrl, getFilters(), null);
		assertEquals(ExplodedJarVisitor.class.getName(), jarVisitor.getClass().getName());		
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
	public void testGetBytesFromInputStream() {
		try {
			File file = buildLargeJar();

			long start = System.currentTimeMillis();
			InputStream stream = new BufferedInputStream(
					new FileInputStream( file ) );
			int oldLength = getBytesFromInputStream( stream ).length;
			stream.close();
			long oldTime = System.currentTimeMillis() - start;

			start = System.currentTimeMillis();
			stream = new BufferedInputStream( new FileInputStream( file ) );
			int newLength = JarVisitorFactory.getBytesFromInputStream(
					stream ).length;
			stream.close();
			long newTime = System.currentTimeMillis() - start;

			assertEquals( oldLength, newLength );
			assertTrue( oldTime > newTime );
		}
		catch ( Exception e ) {
			fail( e.getMessage() );
		}
	}

	// This is the old getBytesFromInputStream from JarVisitorFactory before
	// it was changed by HHH-7835. Use it as a regression test.
	private byte[] getBytesFromInputStream(
			InputStream inputStream) throws IOException {
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
	public void testGetBytesFromZeroInputStream() {
		try {
			// Ensure that JarVisitorFactory#getBytesFromInputStream
			// can handle 0 length streams gracefully.
			InputStream emptyStream = new BufferedInputStream( 
					new FileInputStream( new File(
					"src/test/resources/org/hibernate/ejb/test/packaging/empty.txt" ) ) );
			int length = JarVisitorFactory.getBytesFromInputStream( 
					emptyStream ).length;
			assertEquals( length, 0 );
			emptyStream.close();
		}
		catch ( Exception e ) {
			fail( e.getMessage() );
		}
	}

	private Filter[] getFilters() {
		return new Filter[] {
				new PackageFilter( false, null ) {
					public boolean accept(String javaElementName) {
						return true;
					}
				},
				new ClassFilter(
						false, new Class[] {
								Entity.class,
								MappedSuperclass.class,
								Embeddable.class
						}
				) {
					public boolean accept(String javaElementName) {
						return true;
					}
				},
				new FileFilter( true ) {
					public boolean accept(String javaElementName) {
						return javaElementName.endsWith( "hbm.xml" ) || javaElementName.endsWith( "META-INF/orm.xml" );
					}
				}
		};
	}
}
