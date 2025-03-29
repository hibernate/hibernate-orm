/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.net.MalformedURLException;
import java.net.URL;

import static java.io.File.separatorChar;

/**
 * @author Steve Ebersole
 */
public class TestHelper {
	private static URL RESOLVED_TEST_ROOT_URL;

	public static URL determineTestRootUrl() {
		if ( RESOLVED_TEST_ROOT_URL == null ) {
			RESOLVED_TEST_ROOT_URL = resolveRootUrl( TestHelper.class );
		}
		return RESOLVED_TEST_ROOT_URL;
	}

	public static URL resolveRootUrl(Class knownClass) {
		final String knownClassFileName = '/' + knownClass.getName().replace( '.', separatorChar ) + ".class";
		final URL knownClassFileUrl = TestHelper.class.getResource( knownClassFileName );
		final String knownClassFileUrlString = knownClassFileUrl.toExternalForm();

		// to start, strip off the class file name
		String rootUrlString = knownClassFileUrlString.substring( 0, knownClassFileUrlString.lastIndexOf( separatorChar ) );

		// then strip off each package dir
		final String packageName = knownClass.getPackage().getName();
		for ( String packageNamePart : packageName.split( "\\." ) ) {
			rootUrlString = rootUrlString.substring( 0, rootUrlString.lastIndexOf( separatorChar ) );
		}

		try {
			return new URL( rootUrlString );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( "Could not convert class base url as string to URL ref", e );
		}
	}
}
