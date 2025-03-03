/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.hikaricp.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.HikariCPSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;

import com.zaxxer.hikari.HikariConfig;

import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.consumeSetting;

/**
 * Utility class to map Hibernate properties to HikariCP configuration properties.
 *
 * @author Brett Wooldridge
 * @author Luca Burgazzoli
 * @author Brett Meyer
 */
public class HikariConfigurationUtil {
	public static final String CONFIG_PREFIX = HikariCPSettings.HIKARI_CONFIG_PREFIX + ".";

	/**
	 * Create/load a HikariConfig from Hibernate properties.
	 *
	 * @param properties a map of Hibernate properties
	 * @return a HikariConfig
	 */
	public static HikariConfig loadConfiguration(Map<String,Object> properties) {
		final Properties hikariProps = new Properties();
		copyProperty( JdbcSettings.AUTOCOMMIT, properties, "autoCommit", hikariProps );

		copyProperty( JdbcSettings.POOL_SIZE, properties, "maximumPoolSize", hikariProps );

		copyProperty(
				properties,
				"driverClassName",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_DRIVER,
				JdbcSettings.DRIVER,
				JdbcSettings.JPA_JDBC_DRIVER
		);

		copyProperty(
				properties,
				"jdbcUrl",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_URL,
				JdbcSettings.URL,
				JdbcSettings.JPA_JDBC_URL
		);

		copyProperty(
				properties,
				"username",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_USER,
				JdbcSettings.USER,
				JdbcSettings.JPA_JDBC_USER
		);

		copyProperty(
				properties,
				"password",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_PASSWORD,
				JdbcSettings.PASS,
				JdbcSettings.JPA_JDBC_PASSWORD
		);

		copyIsolationSetting( properties, hikariProps );

		for ( var entry : properties.entrySet() ) {
			final String key = entry.getKey();
			if ( key.startsWith( CONFIG_PREFIX ) ) {
				hikariProps.setProperty( key.substring( CONFIG_PREFIX.length() ), entry.getValue().toString() );
			}
		}

		return new HikariConfig( hikariProps );
	}

	private static void copyProperty(String srcKey, Map<String,Object> src, String dstKey, Properties dst) {
		if ( src.containsKey( srcKey ) ) {
			dst.setProperty( dstKey, src.get( srcKey ).toString() );
		}
	}

	private static void copyProperty(Map<String,Object> src, String dstKey, Properties dst, String... srcKeys) {
		consumeSetting(
				src,
				(name, value) -> dst.setProperty( dstKey, value ),
				srcKeys
		);
	}

	private static void copyIsolationSetting(Map<String,Object> props, Properties hikariProps) {
		final Integer isolation = ConnectionProviderInitiator.extractIsolation( props );
		if ( isolation != null ) {
			hikariProps.put( "transactionIsolation",
					ConnectionProviderInitiator.toIsolationConnectionConstantName( isolation ) );
		}
	}

}
