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
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Christian Beikov
 */
public final class DialectContext {

	private static Dialect dialect;

	static void init() {
		final Properties properties = Environment.getProperties();
		final String diverClassName = properties.getProperty( Environment.DRIVER );
		final String dialectName = properties.getProperty( Environment.DIALECT );
		final String jdbcUrl = properties.getProperty( Environment.URL );
		final Properties props = new Properties();
		props.setProperty( "user", properties.getProperty( Environment.USER ) );
		props.setProperty( "password", properties.getProperty( Environment.PASS ) );
		if ( dialectName == null ) {
			throw new HibernateException( "The dialect was not set. Set the property hibernate.dialect." );
		}
		final Constructor<? extends Dialect> constructor;
		try {
			@SuppressWarnings("unchecked")
			final Class<? extends Dialect> dialectClass = (Class<? extends Dialect>) ReflectHelper.classForName( dialectName );
			constructor = dialectClass.getConstructor( DialectResolutionInfo.class );
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "Dialect class not found: " + dialectName, cnfe );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate given dialect class: " + dialectName, e );
		}
		final Driver driver;
		try {
			driver = (Driver) Class.forName( diverClassName ).newInstance();
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "JDBC Driver class not found: " + dialectName, cnfe );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate given JDBC driver class: " + dialectName, e );
		}
		try ( Connection connection = driver.connect( jdbcUrl, props ) ) {
//			if ( jdbcUrl.startsWith( "jdbc:derby:" ) ) {
//				// Unfortunately we may only configure this once
//				try ( Statement s = connection.createStatement() ) {
//					s.execute( "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(\'derby.locks.waitTimeout\', \'10\')" );
//					if ( !connection.getAutoCommit() ) {
//						connection.commit();
//					}
//				}
//			}
			dialect = constructor.newInstance( new DatabaseMetaDataDialectResolutionInfoAdapter( connection.getMetaData() ) );
		}
		catch (SQLException sqle) {
			throw new JDBCConnectionException( "Could not connect to database with JDBC URL: '"
					+ jdbcUrl + "' [" + sqle.getMessage() + "]", sqle );
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
