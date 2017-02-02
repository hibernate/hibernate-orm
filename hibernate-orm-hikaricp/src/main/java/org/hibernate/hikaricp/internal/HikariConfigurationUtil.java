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
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;

import com.zaxxer.hikari.HikariConfig;

/**
 * Utility class to map Hibernate properties to HikariCP configuration properties.
 * 
 * @author Brett Wooldridge
 * @author Luca Burgazzoli
 * @author Brett Meyer
 */
public class HikariConfigurationUtil {
	public static final String CONFIG_PREFIX = "hibernate.hikari.";

	/**
	 * Create/load a HikariConfig from Hibernate properties.
	 * 
	 * @param props a map of Hibernate properties
	 * @return a HikariConfig
	 */
	@SuppressWarnings("rawtypes")
	public static HikariConfig loadConfiguration(Map props) {
		Properties hikariProps = new Properties();
		copyProperty( AvailableSettings.AUTOCOMMIT, props, "autoCommit", hikariProps );

		copyProperty( AvailableSettings.DRIVER, props, "driverClassName", hikariProps );
		copyProperty( AvailableSettings.URL, props, "jdbcUrl", hikariProps );
		copyProperty( AvailableSettings.USER, props, "username", hikariProps );
		copyProperty( AvailableSettings.PASS, props, "password", hikariProps );

		copyIsolationSetting( props, hikariProps );

		for ( Object keyo : props.keySet() ) {
			if ( !(keyo instanceof String) ) {
				continue;
			}
			String key = (String) keyo;
			if ( key.startsWith( CONFIG_PREFIX ) ) {
				hikariProps.setProperty( key.substring( CONFIG_PREFIX.length() ), (String) props.get( key ) );
			}
		}

		return new HikariConfig( hikariProps );
	}

	@SuppressWarnings("rawtypes")
	private static void copyProperty(String srcKey, Map src, String dstKey, Properties dst) {
		if ( src.containsKey( srcKey ) ) {
			dst.setProperty( dstKey, (String) src.get( srcKey ) );
		}
	}

	private static void copyIsolationSetting(Map props, Properties hikariProps) {
		final Integer isolation = ConnectionProviderInitiator.extractIsolation( props );
		if ( isolation != null ) {
			hikariProps.put(
					"transactionIsolation",
					ConnectionProviderInitiator.toIsolationConnectionConstantName( isolation )
			);
		}
	}

}
