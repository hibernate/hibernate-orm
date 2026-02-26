/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.orm.integrationtest.java.module.test.entity.Author;
import org.hibernate.scan.jandex.IndexBuildingScanner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * We need to test that the scanner works, including when there is a module-info.class
 * resource in the project. See also HHH-13859.
 */
public class ScannerTest {

	@Test
	public void verifyModuleInfoScanner() {
		var urlToThis = Author.class.getProtectionDomain().getCodeSource().getLocation();
		var scanningContext = new ScanningContextImpl(
				StandardArchiveDescriptorFactory.INSTANCE,
				Map.of()
		);
		var scanner = new IndexBuildingScanner( scanningContext );
		var scanResult = scanner.scan( urlToThis );
		assertEquals( 1, scanResult.discoveredClasses().size() );
	}

}
