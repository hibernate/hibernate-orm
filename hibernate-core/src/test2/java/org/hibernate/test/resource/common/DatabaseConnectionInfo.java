/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.common;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

/**
 * @author Steve Ebersole
 */
public class DatabaseConnectionInfo {
	/**
	 * Singleton access
	 */
	public static final DatabaseConnectionInfo INSTANCE = new DatabaseConnectionInfo();

	public static final String DRIVER = "org.h2.Driver";
	public static final String URL = "jdbc:h2:mem:hibernate-core";
	public static final String USER = "hibernate";
	public static final String PASS = "hibernate";

	public Properties createDriverManagerProperties() {
		Properties props = new Properties();
		props.setProperty( AvailableSettings.USER, USER );
		props.setProperty( AvailableSettings.PASS, PASS );
		return props;
	}

	private Driver driver;

	public Connection makeConnection() throws SQLException {
		if ( driver == null ) {
			try {
				driver = (Driver) Class.forName( DRIVER ).newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to load JDBC Driver [" + DRIVER + "]", e );
			}
		}

		return driver.connect( URL, createDriverManagerProperties() );
	}
}
