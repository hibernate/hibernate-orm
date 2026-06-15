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
import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.config.ConfigurationException;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/**
 * Loads configuration properties.
 *
 * @author Steve Ebersole
 */
public class ConfigLoader {

	private final BootstrapServiceRegistry bootstrapServiceRegistry;

	public ConfigLoader(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
	}

	private InputStream locateStream(String cfgXmlResourceName) {
		return bootstrapServiceRegistry.requireService( ClassLoaderService.class )
				.locateResourceStream( cfgXmlResourceName );
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
