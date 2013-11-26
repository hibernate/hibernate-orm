/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static java.io.File.separatorChar;

/**
 * @author Steve Ebersole
 */
public class TestPathHelper {
	/**
	 * Useful in cases where we need to deal with files/resources in the test compilation output dir of the
	 * project.  This gets a reference to the compilation output directory into which the given class was compiled.
	 *
	 * @param knownClass Reference to a Class known to be in the compilation output dir.
	 *
	 * @return The root URL
	 */
	public static URL resolveRootUrl(Class knownClass) {
		final String knownClassFileName = '/' + knownClass.getName().replace( '.', separatorChar ) + ".class";
		final URL knownClassFileUrl = knownClass.getResource( knownClassFileName );
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

	/**
	 * Essentially the same as {@link #resolveRootUrl(Class)}, but here we convert the root URL to a File
	 * (directory) reference.  In fact we delegate to {@link #resolveRootUrl(Class)} and simply convert its
	 * return into a File reference.
	 *
	 * @param knownClass Reference to a Class known to be in the compilation output dir.
	 *
	 * @return The root directory
	 */
	public static File resolveRootDirectory(Class knownClass) {
		try {
			return new File( resolveRootUrl( knownClass ).toURI() );
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Could not convert class root URL to a URI", e );
		}
	}

	/**
	 * Essentially the same as {@link #resolveRootUrl(Class)}, but here we convert the root URL to a File
	 * (directory) reference.  In fact we delegate to {@link #resolveRootUrl(Class)} and simply convert its
	 * return into a File reference.
	 *
	 * @param knownClass Reference to a Class known to be in the compilation output dir.
	 *
	 * @return The root directory
	 */
	public static File resolveClassFile(Class knownClass) {
		final String knownClassFileName = '/' + knownClass.getName().replace( '.', separatorChar ) + ".class";
		final URL knownClassFileUrl = knownClass.getResource( knownClassFileName );

		try {
			return new File( knownClassFileUrl.toURI() );
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Could not convert class root URL to a URI", e );
		}
	}

	private TestPathHelper() {
	}
}
