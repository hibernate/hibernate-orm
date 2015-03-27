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
package org.hibernate.boot.cfgxml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.config.ConfigurationException;
import org.jboss.logging.Logger;

/**
 * Loads {@code cfg.xml} files.
 *
 * @author Steve Ebersole
 */
public class ConfigLoader {
	private static final Logger log = Logger.getLogger( ConfigLoader.class );

	private final BootstrapServiceRegistry bootstrapServiceRegistry;

	private ValueHolder<JaxbCfgProcessor> jaxbProcessorHolder = new ValueHolder<JaxbCfgProcessor>(
			new ValueHolder.DeferredInitializer<JaxbCfgProcessor>() {
				@Override
				public JaxbCfgProcessor initialize() {
					return new JaxbCfgProcessor( bootstrapServiceRegistry.getService( ClassLoaderService.class ) );
				}
			}
	);

	public ConfigLoader(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
	}

	public LoadedConfig loadConfigXmlResource(String cfgXmlResourceName) {
		final InputStream stream = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResourceStream( cfgXmlResourceName );
		if ( stream == null ) {
			throw new ConfigurationException( "Could not locate cfg.xml resource [" + cfgXmlResourceName + "]" );
		}
		final JaxbCfgHibernateConfiguration jaxbCfg = jaxbProcessorHolder.getValue().unmarshal(
				stream,
				new Origin( SourceType.RESOURCE, cfgXmlResourceName )
		);

		return LoadedConfig.consume( jaxbCfg );
	}

	public LoadedConfig loadConfigXmlFile(File cfgXmlFile) {
		try {
			final JaxbCfgHibernateConfiguration jaxbCfg = jaxbProcessorHolder.getValue().unmarshal(
					new FileInputStream( cfgXmlFile ),
					new Origin( SourceType.FILE, cfgXmlFile.getAbsolutePath() )
			);

			return LoadedConfig.consume( jaxbCfg );
		}
		catch (FileNotFoundException e) {
			throw new ConfigurationException(
					"Specified cfg.xml file [" + cfgXmlFile.getAbsolutePath() + "] does not exist"
			);
		}
	}

	public LoadedConfig loadConfigXmlUrl(URL url) {
		try {
			final InputStream stream = url.openStream();
			try {
				final JaxbCfgHibernateConfiguration  jaxbCfg = jaxbProcessorHolder.getValue().unmarshal(
						stream,
						new Origin( SourceType.URL, url.toExternalForm() )
				);

				return LoadedConfig.consume( jaxbCfg );
			}
			finally {
				try {
					stream.close();
				}
				catch (IOException e) {
					log.debug( "Unable to close cfg.xml URL stream", e );
				}
			}
		}
		catch (IOException e) {
			throw new ConfigurationException( "Could not access given cfg.xml URL input stream", e );
		}
	}

	public Properties loadProperties(String resourceName) {
		final InputStream stream = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResourceStream( resourceName );

		if ( stream == null ) {
			throw new ConfigurationException( "Unable to apply settings from properties file [" + resourceName + "]" );
		}

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

	public Properties loadProperties(File file) {
		try {
			final InputStream stream = new FileInputStream( file );
			try {
				Properties properties = new Properties();
				properties.load( stream );
				return properties;
			}
			catch (IOException e) {
				throw new ConfigurationException(
						"Unable to apply settings from properties file [" + file.getAbsolutePath() + "]",
						e
				);
			}
			finally {
				try {
					stream.close();
				}
				catch (IOException e) {
					log.debug(
							String.format( "Unable to close properties file [%s] stream", file.getAbsolutePath() ),
							e
					);
				}
			}
		}
		catch (FileNotFoundException e) {
			throw new ConfigurationException(
					"Unable locate specified properties file [" + file.getAbsolutePath() + "]",
					e
			);
		}
	}

}
