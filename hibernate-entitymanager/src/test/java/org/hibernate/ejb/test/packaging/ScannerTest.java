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
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.MappedSuperclass;
import javax.persistence.Persistence;

import org.junit.Test;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.ejb.test.pack.defaultpar.ApplicationServer;
import org.hibernate.testing.RequiresDialect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@RequiresDialect(H2Dialect.class)
public class ScannerTest extends PackagingTestCase {
	@Test
	public void testNativeScanner() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		Scanner scanner = new NativeScanner();
		assertEquals( "defaultpar", scanner.getUnqualifiedJarName( defaultPar.toURL() ) );

		Set<Class<? extends Annotation>> annotationsToLookFor = new HashSet<Class<? extends Annotation>>( 3 );
		annotationsToLookFor.add( Entity.class );
		annotationsToLookFor.add( MappedSuperclass.class );
		annotationsToLookFor.add( Embeddable.class );
		final Set<Class<?>> classes = scanner.getClassesInJar( defaultPar.toURL(), annotationsToLookFor );

		assertEquals( 3, classes.size() );
		assertTrue( classes.contains( ApplicationServer.class ) );
		assertTrue( classes.contains( org.hibernate.ejb.test.pack.defaultpar.Version.class ) );

		Set<String> filePatterns = new HashSet<String>( 2 );
		filePatterns.add( "**/*.hbm.xml" );
		filePatterns.add( "META-INF/orm.xml" );
		final Set<NamedInputStream> files = scanner.getFilesInJar( defaultPar.toURL(), filePatterns );

		assertEquals( 2, files.size() );
		for ( NamedInputStream file : files ) {
			assertNotNull( file.getStream() );
			file.getStream().close();
		}
	}

	@Test
	public void testCustomScanner() throws Exception {
		File defaultPar = buildDefaultPar();
		File explicitPar = buildExplicitPar();
		addPackageToClasspath( defaultPar, explicitPar );
		
		EntityManagerFactory emf;
		CustomScanner.resetUsed();
		final HashMap integration = new HashMap();
		emf = Persistence.createEntityManagerFactory( "defaultpar", integration );
		assertTrue( ! CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		emf = Persistence.createEntityManagerFactory( "manager1", integration );
		assertTrue( CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		integration.put( AvailableSettings.SCANNER, new CustomScanner() );
		emf = Persistence.createEntityManagerFactory( "defaultpar", integration );
		assertTrue( CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		emf = Persistence.createEntityManagerFactory( "defaultpar", null );
		assertTrue( ! CustomScanner.isUsed() );
		emf.close();
	}
}
