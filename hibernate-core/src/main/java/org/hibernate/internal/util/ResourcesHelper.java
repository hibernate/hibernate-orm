/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Intended for internal use only
 *
 * @author Jan Schatteman
 */
@Internal
public class ResourcesHelper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ResourcesHelper.class );

	private static final String CLASS_PATH_SCHEME = "classpath://";

	public static InputStream locateResourceAsStream(String name, ClassLoader classLoader) {
		if (name == null) {
			return null;
		}

		// first we try name as a URL
		try {
			log.tracef( "trying via [new URL(\"%s\")]", name );
			return new URL( name ).openStream();
		}
		catch (Exception ignore) {
		}

		// if we couldn't find the resource containing a classpath:// prefix above, that means we don't have a URL
		// handler for it. So let's remove the prefix and resolve against our class loader.
		name = stripClasspathScheme( name );

		try {
			log.tracef( "trying via [ClassLoader.getResourceAsStream(\"%s\")]", name );
			final InputStream stream = classLoader.getResourceAsStream( name );
			if ( stream != null ) {
				return stream;
			}
		}
		catch (Exception ignore) {
		}

		if ( name.startsWith( "/" ) ) {
			name = name.substring( 1 );

			try {
				log.tracef( "trying via [new URL(\"%s\")]", name );
				return new URL( name ).openStream();
			}
			catch (Exception ignore) {
			}

			try {
				log.tracef( "trying via [ClassLoader.getResourceAsStream(\"%s\")]", name );
				final InputStream stream = classLoader.getResourceAsStream( name );
				if ( stream != null ) {
					return stream;
				}
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}

	// Convenience method for the Environment class to emulate the search order of ConfigHelper
	public static InputStream locateResourceAsStream(String name, ClassLoader... classLoaders) {
		InputStream inputStream = null;
		for ( ClassLoader classLoader : classLoaders ) {
			inputStream = locateResourceAsStream( name, classLoader );
			if (inputStream != null) {
				break;
			}
		}
		return inputStream;
	}

	public static List<URL> locateResourceAsUrls(String name, ClassLoader classLoader) {
		final List<URL> rtn = new ArrayList<>();

		// first we try name as a URL
		try {
			log.tracef( "trying via [new URL(\"%s\")]", name );
			rtn.add( new URL( name ) );
			return rtn;
		}
		catch (Exception ignore) {
		}

		// if we couldn't find the resource containing a classpath:// prefix above, that means we don't have a URL
		// handler for it. So let's remove the prefix and resolve against our class loader.
		name = stripClasspathScheme( name );

		try {
			final Enumeration<URL> resources = classLoader.getResources( name );

			if ( resources.hasMoreElements() ) {
				while ( resources.hasMoreElements() ) {
					rtn.add( resources.nextElement() );
				}
			}

			if ( name.startsWith( "/" ) ) {
				name = name.substring( 1 );

				try {
					log.tracef( "trying via [ClassLoader.getResource(\"%s\")]", name );
					final Enumeration<URL> strippedResources = classLoader.getResources( name );

					if ( strippedResources.hasMoreElements() ) {
						while ( strippedResources.hasMoreElements() ) {
							rtn.add( strippedResources.nextElement() );
						}
					}
				}
				catch (Exception ignore) {
				}
			}
		}
		catch (IOException ignore) {
		}

		return rtn;
	}

	public static URL locateResourceAsUrl(String name, ClassLoader classLoader) {
		final List<URL> urls = locateResourceAsUrls( name, classLoader );
		if ( urls.isEmpty() ) {
			return null;
		}

		return urls.get( 0 );
	}

	private static String stripClasspathScheme(String name) {
		if ( name == null ) {
			return null;
		}

		if ( name.startsWith( CLASS_PATH_SCHEME ) ) {
			return name.substring( CLASS_PATH_SCHEME.length() );
		}

		return name;
	}
}
