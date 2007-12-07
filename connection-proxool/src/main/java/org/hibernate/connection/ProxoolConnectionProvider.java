/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.ConfigHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.configuration.JAXPConfigurator;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;

/**
 * A connection provider that uses a Proxool connection pool. Hibernate will use this by
 * default if the <tt>hibernate.proxool.*</tt> properties are set.
 * @see ConnectionProvider
 */
public class ProxoolConnectionProvider implements ConnectionProvider {


	private static final String PROXOOL_JDBC_STEM = "proxool.";

	private static final Logger log = LoggerFactory.getLogger(ProxoolConnectionProvider.class);

	private String proxoolAlias;

	// TRUE if the pool is borrowed from the outside, FALSE if we used to create it
	private boolean existingPool;

	// Not null if the Isolation level has been specified in the configuration file.
	// Otherwise, it is left to the Driver's default value.
	private Integer isolation;
	
	private boolean autocommit;

	/**
	 * Grab a connection
	 * @return a JDBC connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
	    // get a connection from the pool (thru DriverManager, cfr. Proxool doc)
		Connection c = DriverManager.getConnection(proxoolAlias);

		// set the Transaction Isolation if defined
		if (isolation!=null) c.setTransactionIsolation( isolation.intValue() );

		// toggle autoCommit to false if set
		if ( c.getAutoCommit()!=autocommit ) c.setAutoCommit(autocommit);

		// return the connection
		return c;
	}

	/**
	 * Dispose of a used connection.
	 * @param conn a JDBC connection
	 * @throws SQLException
	 */
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	/**
	 * Initialize the connection provider from given properties.
	 * @param props <tt>SessionFactory</tt> properties
	 */
	public void configure(Properties props) throws HibernateException {

		// Get the configurator files (if available)
		String jaxpFile = props.getProperty(Environment.PROXOOL_XML);
		String propFile = props.getProperty(Environment.PROXOOL_PROPERTIES);
		String externalConfig = props.getProperty(Environment.PROXOOL_EXISTING_POOL);

		// Default the Proxool alias setting
		proxoolAlias = props.getProperty(Environment.PROXOOL_POOL_ALIAS);

		// Configured outside of Hibernate (i.e. Servlet container, or Java Bean Container
		// already has Proxool pools running, and this provider is to just borrow one of these
		if ( "true".equals(externalConfig) ) {

			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				String msg = "Cannot configure Proxool Provider to use an existing in memory pool without the " + Environment.PROXOOL_POOL_ALIAS + " property set.";
				log.error( msg );
				throw new HibernateException( msg );
			}
			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;

			// Set the existing pool flag to true
			existingPool = true;

			log.info( "Configuring Proxool Provider using existing pool in memory: " + proxoolAlias );

			// Configured using the JAXP Configurator
		}
		else if ( StringHelper.isNotEmpty( jaxpFile ) ) {

			log.info( "Configuring Proxool Provider using JAXPConfigurator: " + jaxpFile );

			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				String msg = "Cannot configure Proxool Provider to use JAXP without the " + Environment.PROXOOL_POOL_ALIAS + " property set.";
				log.error( msg );
				throw new HibernateException( msg );
			}

			try {
				JAXPConfigurator.configure( ConfigHelper.getConfigStreamReader( jaxpFile ), false );
			}
			catch ( ProxoolException e ) {
				String msg = "Proxool Provider unable to load JAXP configurator file: " + jaxpFile;
				log.error( msg, e );
				throw new HibernateException( msg, e );
			}

			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;
			log.info("Configuring Proxool Provider to use pool alias: " + proxoolAlias);

			// Configured using the Properties File Configurator
		}
		else if ( StringHelper.isNotEmpty( propFile ) ) {

			log.info( "Configuring Proxool Provider using Properties File: " + propFile );

			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				String msg = "Cannot configure Proxool Provider to use Properties File without the " + Environment.PROXOOL_POOL_ALIAS + " property set.";
				log.error( msg );
				throw new HibernateException( msg );
			}

			try {
				PropertyConfigurator.configure( ConfigHelper.getConfigProperties( propFile ) );
			}
			catch ( ProxoolException e ) {
				String msg = "Proxool Provider unable to load load Property configurator file: " + propFile;
				log.error( msg, e );
				throw new HibernateException( msg, e );
			}

			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;
			log.info("Configuring Proxool Provider to use pool alias: " + proxoolAlias);
		}

		// Remember Isolation level
		isolation = PropertiesHelper.getInteger(Environment.ISOLATION, props);
		if (isolation!=null) {
		    log.info("JDBC isolation level: " + Environment.isolationLevelToString( isolation.intValue() ) );
		}
		
		autocommit = PropertiesHelper.getBoolean(Environment.AUTOCOMMIT, props);
		log.info("autocommit mode: " + autocommit);
	}

	/**
	 * Release all resources held by this provider. JavaDoc requires a second sentence.
	 * @throws HibernateException
	 */
	public void close() throws HibernateException {

		// If the provider was leeching off an existing pool don't close it
		if (existingPool) {
			return;
		}

		// We have created the pool ourselves, so shut it down
		try {
	        ProxoolFacade.shutdown(0);
		}
		catch (Exception e) {
			// If you're closing down the ConnectionProvider chances are an
			// is not a real big deal, just warn
			log.warn("Exception occured when closing the Proxool pool", e);
			throw new HibernateException("Exception occured when closing the Proxool pool", e);
		}
	}

	/**
	 * @see ConnectionProvider#supportsAggressiveRelease()
	 */
	public boolean supportsAggressiveRelease() {
		return false;
	}

}
