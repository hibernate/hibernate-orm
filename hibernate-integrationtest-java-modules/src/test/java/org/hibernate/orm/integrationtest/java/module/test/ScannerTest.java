/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test;

import java.net.URL;
import java.util.Set;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.internal.StandardScanParameters;
import org.hibernate.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.orm.integrationtest.java.module.test.entity.Author;

import org.junit.Assert;
import org.junit.Test;

/**
 * We need to test that the scanner works, including when there is a module-info.class
 * resource in the project. See also HHH-13859.
 */
public class ScannerTest {

	@Test
	public void verifyModuleInfoScanner() {
		URL urlToThis = Author.class.getProtectionDomain().getCodeSource().getLocation();
		StandardScanner standardScanner = new StandardScanner( StandardArchiveDescriptorFactory.INSTANCE );
		ScanResult scan = standardScanner.scan(
				new TestScanEnvironment( urlToThis ),
				new StandardScanOptions(),
				StandardScanParameters.INSTANCE
		);
		Set<ClassDescriptor> locatedClasses = scan.getLocatedClasses();
		Assert.assertEquals( 1, locatedClasses.size() );
		ClassDescriptor classDescriptor = locatedClasses.iterator().next();
		Assert.assertNotNull( classDescriptor );
		Assert.assertEquals( Author.class.getName(), classDescriptor.getName() );
		Assert.assertEquals( ClassDescriptor.Categorization.MODEL, classDescriptor.getCategorization() );
	}

}
