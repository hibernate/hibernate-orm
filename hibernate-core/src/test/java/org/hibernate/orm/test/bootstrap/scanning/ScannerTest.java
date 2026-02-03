/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.scan.jandex.ProvidedIndexScanner;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ScannerTest extends PackagingTestCase {
	@Test
	public void testNativeScanner() throws Exception {
		var jandexIndex = buildDefaultParIndex();
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		final URL archiveUrl = defaultPar.toURL();

		Scanner scanner = new ProvidedIndexScanner( new ScanningContextTestingImpl(), jandexIndex );
		final ScanningResult scanningResult = scanner.scan( archiveUrl );

	}

	private void assertClassesContained(Set<String> discoveredClasses, Class<?> classToCheckFor) {
		assertThat( discoveredClasses ).contains( classToCheckFor.getName() );
	}

	@Test
	public void testCustomScanner() throws Exception {
		File defaultPar = buildDefaultPar();
		File explicitPar = buildExplicitPar();
		addPackageToClasspath( defaultPar, explicitPar );

		EntityManagerFactory emf;
		CustomScanner.resetUsed();
		emf = new HibernatePersistenceConfiguration( "defaultpar", defaultPar.toURL() )
				.properties( ServiceRegistryUtil.createBaseSettings() )
				.createEntityManagerFactory();
		assertFalse( CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		emf = new HibernatePersistenceConfiguration( "manager1", explicitPar.toURL() )
				.properties( ServiceRegistryUtil.createBaseSettings() )
				.createEntityManagerFactory();
		assertFalse( CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		emf = new HibernatePersistenceConfiguration( "defaultpar", defaultPar.toURL() )
				.properties( ServiceRegistryUtil.createBaseSettings() )
				.property( AvailableSettings.SCANNER, new CustomScanner() )
				.createEntityManagerFactory();
		assertTrue( CustomScanner.isUsed() );
		emf.close();

		CustomScanner.resetUsed();
		emf = new HibernatePersistenceConfiguration( "defaultpar", defaultPar.toURL() )
				.properties( ServiceRegistryUtil.createBaseSettings() )
				.createEntityManagerFactory();
		assertFalse( CustomScanner.isUsed() );
		emf.close();
	}

}
