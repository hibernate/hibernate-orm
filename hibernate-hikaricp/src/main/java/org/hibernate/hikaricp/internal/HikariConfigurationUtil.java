/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.hikaricp.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;

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
		copyProperty( AvailableSettings.ISOLATION, props, "transactionIsolation", hikariProps );
		copyProperty( AvailableSettings.AUTOCOMMIT, props, "autoCommit", hikariProps );

		copyProperty(AvailableSettings.DRIVER, props, "driverClassName", hikariProps);
		copyProperty(AvailableSettings.URL, props, "jdbcUrl", hikariProps);
		copyProperty( AvailableSettings.USER, props, "username", hikariProps );
		copyProperty( AvailableSettings.PASS, props, "password", hikariProps );

		for ( Object keyo : props.keySet() ) {
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
}
