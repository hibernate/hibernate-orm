/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.hikaricp.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
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
	 * @param props a map of Hibernate properties
	 * @return a HikariConfig
	 */
	public static HikariConfig loadConfiguration(Map<String,Object> props) {
		Properties hikariProps = new Properties();
		copyProperty( JdbcSettings.AUTOCOMMIT, props, "autoCommit", hikariProps );

		copyProperty(
				props,
				"driverClassName",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_DRIVER,
				JdbcSettings.DRIVER,
				JdbcSettings.JPA_JDBC_DRIVER
		);

		copyProperty(
				props,
				"jdbcUrl",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_URL,
				JdbcSettings.URL,
				JdbcSettings.JPA_JDBC_URL
		);

		copyProperty(
				props,
				"username",
				hikariProps,
				JdbcSettings.JAKARTA_JDBC_USER,
				JdbcSettings.USER,
				JdbcSettings.JPA_JDBC_USER
		);

		copyProperty(
				props,
				"password",
				hikariProps,
				AvailableSettings.JAKARTA_JDBC_PASSWORD,
				AvailableSettings.PASS,
				AvailableSettings.JPA_JDBC_PASSWORD
		);

		copyIsolationSetting( props, hikariProps );

		for ( String key : props.keySet() ) {
			if ( key.startsWith( CONFIG_PREFIX ) ) {
				hikariProps.setProperty( key.substring( CONFIG_PREFIX.length() ), (String) props.get( key ) );
			}
		}

		return new HikariConfig( hikariProps );
	}

	private static void copyProperty(String srcKey, Map<String,Object> src, String dstKey, Properties dst) {
		if ( src.containsKey( srcKey ) ) {
			dst.setProperty( dstKey, (String) src.get( srcKey ) );
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
			hikariProps.put(
					"transactionIsolation",
					ConnectionProviderInitiator.toIsolationConnectionConstantName( isolation )
			);
		}
	}

}
