/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Christian Beikov
 */
public final class DialectContext {

	private static Dialect dialect;

	static void init() {
		final Properties properties = Environment.getProperties();
		final String dialectName = properties.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			throw new HibernateException( "The dialect was not set. Set the property hibernate.dialect." );
		}
		try {
			final Class<? extends Dialect> dialectClass = ReflectHelper.classForName( dialectName );
			final Constructor<? extends Dialect> constructor = dialectClass.getConstructor( DialectResolutionInfo.class );
			Driver driver = (Driver) Class.forName( properties.getProperty( Environment.DRIVER ) ).newInstance();
			Properties props = new Properties();
			props.setProperty( "user", properties.getProperty( Environment.USER ) );
			props.setProperty( "password", properties.getProperty( Environment.PASS ) );
			try (Connection connection = driver.connect( properties.getProperty( Environment.URL ), props )) {
				dialect = constructor.newInstance( new DatabaseMetaDataDialectResolutionInfoAdapter( connection.getMetaData() ) );
			}
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "Dialect class not found: " + dialectName, cnfe );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate given dialect class: " + dialectName, e );
		}
	}

	private DialectContext() {
	}

	public static synchronized Dialect getDialect() {
		if (dialect==null) {
			init();
		}
		return dialect;
	}
}
