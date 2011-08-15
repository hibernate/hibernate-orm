/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.integrator.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ServiceConfigurationError;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Strong Liu
 */
final class ServiceLoader<S> implements Iterable<S> {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, ServiceLoader.class.getName()
	);
	private static final String PREFIX = "META-INF/services/";
	private LinkedHashMap<String, S> providers = new LinkedHashMap<String, S>();

	private ServiceLoader(Class<S> svc, ServiceRegistry serviceRegistry) {
		Class<S> service = svc;
		ClassLoaderService loader = serviceRegistry.getService( ClassLoaderService.class );
		String fullName = PREFIX + service.getName();
		service.getName();

		List<URL> configs = locateResources( loader, fullName );
		for ( URL url : configs ) {
			Iterator<String> names = parse( service, url );
			while ( names.hasNext() ) {
				String cn = names.next();
				try {
					S p = service.cast( loader.classForName( cn ).newInstance() );
					providers.put( cn, p );
				}
				catch ( Throwable x ) {
					fail( service, "Provider " + cn + " could not be instantiated: " + x, x );
				}
			}

		}
	}

	private List<URL> locateResources(ClassLoaderService loader, String fullName) {
		List<URL> urls = new ArrayList<URL>();
		urls.addAll( loader.locateResources( fullName ) );
		try {
			Enumeration<URL> hibUrls = ServiceLoader.class.getClassLoader().getResources( fullName );
			while ( hibUrls.hasMoreElements() ) {
				URL u = hibUrls.nextElement();
				if ( !urls.contains( u ) ) {
					urls.add( u );
				}

			}
		}
		catch ( IOException e ) {
			//ignore
		}
		return urls;
	}

	private static void fail(Class service, String msg, Throwable cause)
			throws ServiceConfigurationError {
		throw new ServiceConfigurationError(
				service.getName() + ": " + msg,
				cause
		);
	}

	private static void fail(Class service, String msg)
			throws ServiceConfigurationError {
		throw new ServiceConfigurationError( service.getName() + ": " + msg );
	}

	private static void fail(Class service, URL u, int line, String msg)
			throws ServiceConfigurationError {
		fail( service, u + ":" + line + ": " + msg );
	}

	// Parse a single line from the given configuration file, adding the name
	// on the line to the names list.
	//
	private int parseLine(Class service, URL u, BufferedReader r, int lc,
						  List<String> names)
			throws IOException, ServiceConfigurationError {
		String ln = r.readLine();
		if ( ln == null ) {
			return -1;
		}
		int ci = ln.indexOf( '#' );
		if ( ci >= 0 ) {
			ln = ln.substring( 0, ci );
		}
		ln = ln.trim();
		int n = ln.length();
		if ( n != 0 ) {
			if ( ( ln.indexOf( ' ' ) >= 0 ) || ( ln.indexOf( '\t' ) >= 0 ) ) {
				fail( service, u, lc, "Illegal configuration-file syntax" );
			}
			int cp = ln.codePointAt( 0 );
			if ( !Character.isJavaIdentifierStart( cp ) ) {
				fail( service, u, lc, "Illegal provider-class name: " + ln );
			}
			for ( int i = Character.charCount( cp ); i < n; i += Character.charCount( cp ) ) {
				cp = ln.codePointAt( i );
				if ( !Character.isJavaIdentifierPart( cp ) && ( cp != '.' ) ) {
					fail( service, u, lc, "Illegal provider-class name: " + ln );
				}
			}
			if ( !providers.containsKey( ln ) && !names.contains( ln ) ) {
				names.add( ln );
			}
		}
		return lc + 1;
	}

	private Iterator<String> parse(Class service, URL u)
			throws ServiceConfigurationError {
		InputStream in = null;
		BufferedReader r = null;
		ArrayList<String> names = new ArrayList<String>();
		try {
			in = u.openStream();
			r = new BufferedReader( new InputStreamReader( in, "utf-8" ) );
			int lc = 1;
			while ( ( lc = parseLine( service, u, r, lc, names ) ) >= 0 ) {
				;
			}
		}
		catch ( IOException x ) {
			fail( service, "Error reading configuration file", x );
		}
		finally {
			try {
				if ( r != null ) {
					r.close();
				}
				if ( in != null ) {
					in.close();
				}
			}
			catch ( IOException y ) {
				fail( service, "Error closing configuration file", y );
			}
		}
		return names.iterator();
	}

	public Iterator<S> iterator() {
		return providers.values().iterator();

	}

	public static <S> ServiceLoader<S> load(Class<S> service,
											ServiceRegistry serviceRegistry) {
		return new ServiceLoader<S>( service, serviceRegistry );
	}


}
