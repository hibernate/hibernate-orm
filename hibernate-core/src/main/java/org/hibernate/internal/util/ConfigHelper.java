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

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;


/**
 * A simple class to centralize logic needed to locate config files on the system.
 *
 * @todo : Update usages to use {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService}
 *
 * @author Steve Ebersole
 */
public final class ConfigHelper {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, ConfigHelper.class.getName());

	/** Try to locate a local URL representing the incoming path.  The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this URL as a java system
	 * resource through {@link ClassLoaderService}.
	 *
	 * @param path The path representing the config location.
	 * @param classLoaderService The ClassLoaderService.
	 * @return An appropriate URL or null.
	 */
	public static URL locateConfig(final String path, final ClassLoaderService classLoaderService) {
		try {
			return new URL(path);
		}
		catch(MalformedURLException e) {
			return classLoaderService.locateResource( path );
		}
	}

	/** Open an InputStream to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain the stream.
	 *
	 * @param path The path representing the config location.
	 * @param classLoaderService The ClassLoaderService.
	 * @return An input stream to the requested config resource.
	 * @throws HibernateException Unable to open stream to that resource.
	 */
	public static InputStream getConfigStream(final String path,
			final ClassLoaderService classLoaderService) throws HibernateException {
		final URL url = ConfigHelper.locateConfig(path, classLoaderService);

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
	 * @param classLoaderService The ClassLoaderService.
	 * @return An input stream to the requested config resource.
	 * @throws HibernateException Unable to open reader to that resource.
	 */
	public static Reader getConfigStreamReader(final String path,
			final ClassLoaderService classLoaderService) throws HibernateException {
		return new InputStreamReader( getConfigStream(path, classLoaderService) );
	}

	/** Loads a properties instance based on the data at the incoming config location.
	 *
	 * @param path The path representing the config location.
	 * @param classLoaderService The ClassLoaderService.
	 * @return The loaded properties instance.
	 * @throws HibernateException Unable to load properties from that resource.
	 */
	public static Properties getConfigProperties(String path,
			final ClassLoaderService classLoaderService) throws HibernateException {
		try {
			Properties properties = new Properties();
			properties.load( getConfigStream(path, classLoaderService) );
			return properties;
		}
		catch(IOException e) {
			throw new HibernateException("Unable to load properties from specified config file: " + path, e);
		}
	}

	private ConfigHelper() {}

	/**
	 * TODO: Kept only for legacy ORM 4 callers.  Remove in ORM 5.
	 * @deprecated Replace with direct use of {@link ClassLoaderService}.
	 */
	@Deprecated
	public static InputStream getResourceAsStream(String resource) {
		String stripped = resource.startsWith("/") ?
				resource.substring(1) : resource;

		InputStream stream = null;
		ClassLoader classLoader = ClassLoaderHelper.getContextClassLoader();
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

	/**
	 * TODO: Kept only for legacy ORM 4 callers.  Remove in ORM 5.
	 * @deprecated Replace with direct use of {@link ClassLoaderService}.
	 */
	@Deprecated
	public static InputStream getUserResourceAsStream(String resource) {
		boolean hasLeadingSlash = resource.startsWith( "/" );
		String stripped = hasLeadingSlash ? resource.substring(1) : resource;

		InputStream stream = null;

		ClassLoader classLoader = ClassLoaderHelper.getContextClassLoader();
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
