/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.config.ConfigurationException;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.jaxb.SourceType.FILE;
import static org.hibernate.boot.jaxb.SourceType.RESOURCE;
import static org.hibernate.boot.jaxb.SourceType.URL;

/**
 * Loads {@code cfg.xml} files.
 *
 * @author Steve Ebersole
 */
public class ConfigLoader {

	private final BootstrapServiceRegistry bootstrapServiceRegistry;

	private final ValueHolder<JaxbCfgProcessor> jaxbProcessorHolder = new ValueHolder<>(
			new ValueHolder.DeferredInitializer<>() {
				@Override
				public JaxbCfgProcessor initialize() {
					return new JaxbCfgProcessor( bootstrapServiceRegistry.getService( ClassLoaderService.class ) );
				}
			}
	);

	public ConfigLoader(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
	}

	private InputStream locateStream(String cfgXmlResourceName) {
		return bootstrapServiceRegistry.requireService( ClassLoaderService.class )
				.locateResourceStream( cfgXmlResourceName );
	}

	public LoadedConfig loadConfigXmlResource(String cfgXmlResourceName) {
		final var stream = locateStream( cfgXmlResourceName );
		if ( stream == null ) {
			throw new ConfigurationException( "Could not locate cfg.xml resource [" + cfgXmlResourceName + "]" );
		}

		try {
			return LoadedConfig.consume( jaxbProcessorHolder.getValue()
					.unmarshal( stream, new Origin( RESOURCE, cfgXmlResourceName ) ) );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				BOOT_LOGGER.unableToCloseCfgXmlResourceStream( e );
			}
		}
	}

	public LoadedConfig loadConfigXmlFile(File cfgXmlFile) {
		try {

			return LoadedConfig.consume( jaxbProcessorHolder.getValue()
					.unmarshal( new FileInputStream( cfgXmlFile ),
							new Origin( FILE, cfgXmlFile.getAbsolutePath() ) ) );
		}
		catch (FileNotFoundException e) {
			throw new ConfigurationException(
					"Specified cfg.xml file [" + cfgXmlFile.getAbsolutePath() + "] does not exist"
			);
		}
	}

	public LoadedConfig loadConfigXmlUrl(URL url) {
		try {
			final var stream = url.openStream();
			try {
				return LoadedConfig.consume( jaxbProcessorHolder.getValue()
						.unmarshal( stream, new Origin( URL, url.toExternalForm() ) ) );
			}
			finally {
				try {
					stream.close();
				}
				catch (IOException e) {
					BOOT_LOGGER.unableToCloseCfgXmlUrlStream( e );
				}
			}
		}
		catch (IOException e) {
			throw new ConfigurationException( "Could not access given cfg.xml URL input stream", e );
		}
	}

	public Properties loadProperties(String resourceName) {
		final var stream = locateStream( resourceName );
		if ( stream == null ) {
			throw new ConfigurationException( "Unable to apply settings from properties file [" + resourceName + "]" );
		}

		try {
			return loadProperties( stream );
		}
		catch (IOException e) {
			throw new ConfigurationException( "Unable to apply settings from properties file [" + resourceName + "]", e );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				BOOT_LOGGER.unableToClosePropertiesFileStream( resourceName, e );
			}
		}
	}

	public Properties loadProperties(File file) {
		try {
			final var stream = new FileInputStream( file );
			try {
				return loadProperties( stream );
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
					BOOT_LOGGER.unableToClosePropertiesFileStream( file.getAbsolutePath(), e );
				}
			}
		}
		catch (FileNotFoundException e) {
			throw new ConfigurationException(
					"Unable to locate specified properties file [" + file.getAbsolutePath() + "]",
					e
			);
		}
	}

	private static Properties loadProperties(InputStream stream) throws IOException {
		final var properties = new Properties();
		properties.load( stream );
		return properties;
	}
}
