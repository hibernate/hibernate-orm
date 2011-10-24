/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A simple class to centralize logic needed to locate config files on the system.
 *
 * @todo : Update usages to use {@link org.hibernate.service.classloading.spi.ClassLoaderService}
 *
 * @author Steve Ebersole
 */
public final class ConfigHelper {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, ConfigHelper.class.getName());

	/** Try to locate a local URL representing the incoming path.  The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this UURL as a java system
	 * resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
	public static URL locateConfig(final String path) {
		try {
			return new URL(path);
		}
		catch(MalformedURLException e) {
			return findAsResource(path);
		}
	}

	/**
	 * Try to locate a local URL representing the incoming path.
	 * This method <b>only</b> attempts to locate this URL as a
	 * java system resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
	public static URL findAsResource(final String path) {
		URL url = null;

		// First, try to locate this resource through the current
		// context classloader.
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if (contextClassLoader!=null) {
			url = contextClassLoader.getResource(path);
		}
		if (url != null)
			return url;

		// Next, try to locate this resource through this class's classloader
		url = ConfigHelper.class.getClassLoader().getResource(path);
		if (url != null)
			return url;

		// Next, try to locate this resource through the system classloader
		url = ClassLoader.getSystemClassLoader().getResource(path);

		// Anywhere else we should look?
		return url;
	}

	/** Open an InputStream to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain the stream.
	 *
	 * @param path The path representing the config location.
	 * @return An input stream to the requested config resource.
	 * @throws HibernateException Unable to open stream to that resource.
	 */
	public static InputStream getConfigStream(final String path) throws HibernateException {
		final URL url = ConfigHelper.locateConfig(path);

		if (url == null) {
            String msg = LOG.unableToLocateConfigFile(path);
            LOG.error(msg);
			throw new HibernateException(msg);
		}

		try {
			return url.openStream();
        }
		catch(IOException e) {
	        throw new HibernateException("Unable to open config file: " + path, e);
        }
	}

	/** Open an Reader to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain a stream, which is then
	 * wrapped in a Reader.
	 *
	 * @param path The path representing the config location.
	 * @return An input stream to the requested config resource.
	 * @throws HibernateException Unable to open reader to that resource.
	 */
	public static Reader getConfigStreamReader(final String path) throws HibernateException {
		return new InputStreamReader( getConfigStream(path) );
	}

	/** Loads a properties instance based on the data at the incoming config location.
	 *
	 * @param path The path representing the config location.
	 * @return The loaded properties instance.
	 * @throws HibernateException Unable to load properties from that resource.
	 */
	public static Properties getConfigProperties(String path) throws HibernateException {
		try {
			Properties properties = new Properties();
			properties.load( getConfigStream(path) );
			return properties;
		}
		catch(IOException e) {
			throw new HibernateException("Unable to load properties from specified config file: " + path, e);
		}
	}

	private ConfigHelper() {}

	public static InputStream getResourceAsStream(String resource) {
		String stripped = resource.startsWith("/") ?
				resource.substring(1) : resource;

		InputStream stream = null;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader!=null) {
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
		String stripped = hasLeadingSlash ? resource.substring(1) : resource;

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
