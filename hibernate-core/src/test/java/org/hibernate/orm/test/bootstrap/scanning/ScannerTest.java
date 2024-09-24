/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.internal.StandardScanParameters;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.StandardJpaScanEnvironmentImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer;
import org.hibernate.orm.test.jpa.pack.defaultpar.Version;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;


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
				StandardScanParameters.INSTANCE
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
		final Map<String, Object> integration = ServiceRegistryUtil.createBaseSettings();
		emf = Persistence.createEntityManagerFactory( "defaultpar", integration );
		assertTrue( !CustomScanner.isUsed() );
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
		emf = Persistence.createEntityManagerFactory( "defaultpar", ServiceRegistryUtil.createBaseSettings() );
		assertTrue( !CustomScanner.isUsed() );
		emf.close();
	}

	@Test
	@JiraKey("HHH-16840")
	public void testScanResultSerialization() throws Exception {
		final File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		final PersistenceUnitDescriptor descriptor = new ParsedPersistenceXmlDescriptor( defaultPar.toURL() );
		final ScanEnvironment env = new StandardJpaScanEnvironmentImpl( descriptor );
		final ScanOptions options = new StandardScanOptions( "hbm,class", descriptor.isExcludeUnlistedClasses() );
		final Scanner scanner = new StandardScanner();
		final ScanResult scanResult = scanner.scan(
				env,
				options,
				StandardScanParameters.INSTANCE
		);

		validateDefaultParScanResult( scanResult );

		final ScanResult scanResultClone = (ScanResult) SerializationHelper.clone( (Serializable) scanResult );
		assertThat( scanResultClone ).isNotSameAs( scanResult );
		validateDefaultParScanResult( scanResultClone );
	}

	private void validateDefaultParScanResult(ScanResult scanResult) {
		assertThat( scanResult ).isNotNull();

		assertThat( scanResult.getLocatedClasses() ).hasSize( 3 );
		assertThat( scanResult.getLocatedClasses() ).allSatisfy( descriptor -> {
			assertThat( descriptor.getStreamAccess() ).isNotNull();
			assertThat( descriptor.getCategorization() ).isNotNull();
			assertThat( descriptor.getName() ).isNotBlank();
		} );

		assertThat( scanResult.getLocatedMappingFiles() ).hasSize( 2 );
		assertThat( scanResult.getLocatedMappingFiles() ).allSatisfy( descriptor -> {
			assertThat( descriptor.getStreamAccess() ).isNotNull();
			assertThat( descriptor.getName() ).isNotBlank();
		} );

		assertThat( scanResult.getLocatedPackages() ).hasSize( 1 );
		assertThat( scanResult.getLocatedPackages() ).allSatisfy( descriptor -> {
			assertThat( descriptor.getStreamAccess() ).isNotNull();
			assertThat( descriptor.getName() ).isNotBlank();
		} );
	}
}
