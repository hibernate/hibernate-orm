/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

public class ConnectionConfigurationHelper {

	public static Object getDataSource(Map<String, Object> configValues) {
		final Object dataSource = configValues.get( Environment.DATASOURCE );
		if ( dataSource == null ) {
			final Object jtaDatasource = configValues.get( Environment.JAKARTA_JTA_DATASOURCE );
			if ( jtaDatasource != null ) {
				return jtaDatasource;
			}
			return configValues.get( Environment.JAKARTA_NON_JTA_DATASOURCE );
		}
		return dataSource;
	}

	public static String getPassword(Map<String, Object> configValues) {
		final String password = (String) configValues.get( Environment.PASS );
		if ( password != null ) {
			return password;
		}
		return (String) configValues.get( Environment.JAKARTA_JDBC_PASSWORD );
	}

	public static String getUser(Map<String, Object> configValues) {
		final String user = (String) configValues.get( Environment.USER );
		if ( user != null ) {
			return user;
		}
		return (String) configValues.get( Environment.JAKARTA_JDBC_USER );
	}

	public static String getDriverClassName(Map<String, Object> configValues) {
		final String driverClassName = (String) configValues.get( AvailableSettings.DRIVER );
		if ( driverClassName != null ) {
			return driverClassName;
		}
		return (String) configValues.get( AvailableSettings.JAKARTA_JDBC_DRIVER );
	}

	public static String getUrl(Map<String, Object> configValues) {
		final String url = (String) configValues.get( AvailableSettings.URL );
		if ( url != null ) {
			return url;
		}
		return (String) configValues.get( AvailableSettings.JAKARTA_JDBC_URL );
	}

}
