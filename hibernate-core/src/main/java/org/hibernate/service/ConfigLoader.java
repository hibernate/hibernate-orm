/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;
import org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.service.internal.JaxbProcessor;

import org.jboss.logging.Logger;

/**
 * Loads {@code cfg.xml} files.
 *
 * @author Steve Ebersole
 */
public class ConfigLoader {
	private static final Logger log = Logger.getLogger( ConfigLoader.class );

	private final BootstrapServiceRegistry bootstrapServiceRegistry;

	private ValueHolder<JaxbProcessor> jaxbProcessorHolder = new ValueHolder<JaxbProcessor>(
			new ValueHolder.DeferredInitializer<JaxbProcessor>() {
				@Override
				public JaxbProcessor initialize() {
					return new JaxbProcessor( bootstrapServiceRegistry.getService( ClassLoaderService.class ) );
				}
			}
	);

	public ConfigLoader(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
	}

	public JaxbHibernateConfiguration loadConfigXmlResource(String cfgXmlResourceName) {
		final InputStream stream = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResourceStream( cfgXmlResourceName );
		if ( stream == null ) {
			throw new ConfigurationException( "Could not locate cfg.xml resource [" + cfgXmlResourceName + "]" );
		}
		return unmarshall( stream, new Origin( SourceType.RESOURCE, cfgXmlResourceName ) );
	}

	private JaxbHibernateConfiguration unmarshall(InputStream stream, Origin origin) {
		try {
			return jaxbProcessorHolder.getValue().unmarshal( stream, origin );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				log.debug( "Unable to close config input stream : " + e.getMessage() );
			}
		}
	}

	public JaxbHibernateConfiguration loadConfigFile(File cfgXmlFile) {
		final InputStream stream = toStream( cfgXmlFile );
		return unmarshall( stream, new Origin( SourceType.FILE, cfgXmlFile.getAbsolutePath() ) );
	}

	private InputStream toStream(File file) {
		try {
			return new FileInputStream( file );
		}
		catch (FileNotFoundException e) {
			throw new ConfigurationException(
					"Could not open input stream from File [" + file.getAbsolutePath() + "]"
			);
		}
	}

	public JaxbHibernateConfiguration loadConfig(URL configFileUrl) {
		final InputStream stream = toStream( configFileUrl );
		return unmarshall( stream, new Origin( SourceType.URL, configFileUrl.toExternalForm() ) );
	}

	private InputStream toStream(URL configFileUrl) {
		try {
			return configFileUrl.openStream();
		}
		catch (IOException e) {
			throw new ConfigurationException(
					"Could not open input stream from config file url [" + configFileUrl.toExternalForm() + "]"
			);
		}
	}

	public Properties loadProperties(String resourceName) {
		final InputStream stream = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResourceStream( resourceName );
		try {
			Properties properties = new Properties();
			properties.load( stream );
			return properties;
		}
		catch (IOException e) {
			throw new ConfigurationException( "Unable to apply settings from properties file [" + resourceName + "]", e );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				log.debug(
						String.format( "Unable to close properties file [%s] stream", resourceName ),
						e
				);
			}
		}
	}

	public Properties loadProperties(File propertyFile) {
		final InputStream stream = toStream( propertyFile );
		try {
			Properties properties = new Properties();
			properties.load( stream );
			return properties;
		}
		catch (IOException e) {
			throw new ConfigurationException( "Unable to apply settings from properties file [" + propertyFile + "]", e );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				log.debug(
						String.format( "Unable to close properties file [%s] stream", propertyFile ),
						e
				);
			}
		}
	}
}
