// $Id$
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

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

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JarVisitorTest extends PackagingTestCase {

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

	/**
	 * EJB-230
	 */
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
