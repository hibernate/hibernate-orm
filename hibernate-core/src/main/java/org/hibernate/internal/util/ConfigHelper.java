/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;

/**
 * A simple class to centralize logic needed to locate config files on the system.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService} instead
 */
@Deprecated
public final class ConfigHelper {

	/**
	 * Try to locate a local URL representing the incoming path.  The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this URL as a java system
	 * resource.
	 *
	 * @param path The path representing the config location.
	 *
	 * @return An appropriate URL or null.
	 */
	public static URL locateConfig(final String path) {
		try {
			return new URL( path );
		}
		catch (MalformedURLException e) {
			return findAsResource( path );
		}
	}

	/**
	 * Try to locate a local URL representing the incoming path.
	 * This method <b>only</b> attempts to locate this URL as a
	 * java system resource.
	 *
	 * @param path The path representing the config location.
	 *
	 * @return An appropriate URL or null.
	 */
	public static URL findAsResource(final String path) {
		URL url = null;

		// First, try to locate this resource through the current
		// context classloader.
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if ( contextClassLoader != null ) {
			url = contextClassLoader.getResource( path );
		}
		if ( url != null ) {
			return url;
		}

		// Next, try to locate this resource through this class's classloader
		url = ConfigHelper.class.getClassLoader().getResource( path );
		if ( url != null ) {
			return url;
		}

		// Next, try to locate this resource through the system classloader
		url = ClassLoader.getSystemClassLoader().getResource( path );

		// Anywhere else we should look?
		return url;
	}

	/**
	 * Open an InputStream to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(String)} in order to find an appropriate URL.
	 * {@link URL#openStream()} is then called to obtain the stream.
	 *
	 * @param path The path representing the config location.
	 *
	 * @return An input stream to the requested config resource.
	 *
	 * @throws HibernateException Unable to open stream to that resource.
	 */
	public static InputStream getConfigStream(final String path) throws HibernateException {
		final URL url = locateConfig( path );
		if ( url == null ) {
			throw new HibernateException( "Unable to locate config file: " + path );
		}
		try {
			return url.openStream();
		}
		catch (IOException e) {
			throw new HibernateException( "Unable to open config file: " + path, e );
		}
	}

	private ConfigHelper() {
	}

	public static InputStream getResourceAsStream(String resource) {
		final String stripped = resource.startsWith( "/" )
				? resource.substring( 1 )
				: resource;

		InputStream stream = null;
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader != null ) {
			stream = classLoader.getResourceAsStream( stripped );
		}
		if ( stream == null ) {
			stream = Environment.class.getResourceAsStream( resource );
		}
		if ( stream == null ) {
			stream = Environment.class.getClassLoader().getResourceAsStream( stripped );
		}
		if ( stream == null ) {
			throw new HibernateException( resource + " not found" );
		}
		return stream;
	}


	public static InputStream getUserResourceAsStream(String resource) {
		boolean hasLeadingSlash = resource.startsWith( "/" );
		final String stripped = hasLeadingSlash ? resource.substring( 1 ) : resource;

		InputStream stream = null;

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader != null ) {
			stream = classLoader.getResourceAsStream( resource );
			if ( stream == null && hasLeadingSlash ) {
				stream = classLoader.getResourceAsStream( stripped );
			}
		}

		if ( stream == null ) {
			stream = Environment.class.getClassLoader().getResourceAsStream( resource );
		}
		if ( stream == null && hasLeadingSlash ) {
			stream = Environment.class.getClassLoader().getResourceAsStream( stripped );
		}

		if ( stream == null ) {
			throw new HibernateException( resource + " not found" );
		}

		return stream;
	}
}
