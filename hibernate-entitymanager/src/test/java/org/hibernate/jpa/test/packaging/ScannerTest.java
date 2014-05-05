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

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.StandardJpaScanEnvironmentImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.pack.defaultpar.ApplicationServer;
import org.hibernate.jpa.test.pack.defaultpar.Version;
import org.hibernate.metamodel.archive.scan.internal.StandardScanOptions;
import org.hibernate.metamodel.archive.scan.internal.StandardScanner;
import org.hibernate.metamodel.archive.scan.spi.ClassDescriptor;
import org.hibernate.metamodel.archive.scan.spi.JandexInitializer;
import org.hibernate.metamodel.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;
import org.hibernate.metamodel.archive.scan.spi.ScanOptions;
import org.hibernate.metamodel.archive.scan.spi.ScanParameters;
import org.hibernate.metamodel.archive.scan.spi.ScanResult;
import org.hibernate.metamodel.archive.scan.spi.Scanner;
import org.hibernate.metamodel.internal.JandexInitManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ScannerTest extends PackagingTestCase {
	@Test
	public void testNativeScanner() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		PersistenceUnitDescriptor descriptor = new ParsedPersistenceXmlDescriptor( defaultPar.toURL() );
		ScanEnvironment env = new StandardJpaScanEnvironmentImpl( descriptor );
		ScanOptions options = new StandardScanOptions( "hbm,class", descriptor.isExcludeUnlistedClasses() );
		Scanner scanner = new StandardScanner();
		ScanResult scanResult = scanner.scan(
				env,
				options,
				new ScanParameters() {
					private final JandexInitManager jandexInitManager = new JandexInitManager();
					@Override
					public JandexInitializer getJandexInitializer() {
						return jandexInitManager;
					}
				}
		);

		assertEquals( 3, scanResult.getLocatedClasses().size() );
		assertClassesContained( scanResult, ApplicationServer.class );
		assertClassesContained( scanResult, Version.class );

		assertEquals( 2, scanResult.getLocatedMappingFiles().size() );
		for ( MappingFileDescriptor mappingFileDescriptor : scanResult.getLocatedMappingFiles() ) {
			assertNotNull( mappingFileDescriptor.getName() );
			assertNotNull( mappingFileDescriptor.getStreamAccess() );
			InputStream stream = mappingFileDescriptor.getStreamAccess().accessInputStream();
			assertNotNull( stream );
			stream.close();
		}
	}

	private void assertClassesContained(ScanResult scanResult, Class classToCheckFor) {
		for ( ClassDescriptor classDescriptor : scanResult.getLocatedClasses() ) {
			if ( classDescriptor.getName().equals( classToCheckFor.getName() ) ) {
				return;
			}
		}
		fail( "ScanResult did not contain expected Class : " + classToCheckFor.getName() );
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
