/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxool.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.configuration.JAXPConfigurator;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;

/**
 * A connection provider that uses a Proxool connection pool. Hibernate will use this by
 * default if the <tt>hibernate.proxool.*</tt> properties are set.
 *
 * @see ConnectionProvider
 */
public class ProxoolConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {
	private static final ProxoolMessageLogger LOG = Logger.getMessageLogger(
			ProxoolMessageLogger.class,
			ProxoolConnectionProvider.class.getName()
	);

	private static final String PROXOOL_JDBC_STEM = "proxool.";

	private String proxoolAlias;

	// TRUE if the pool is borrowed from the outside, FALSE if we used to create it
	private boolean existingPool;

	// Not null if the Isolation level has been specified in the configuration file.
	// Otherwise, it is left to the Driver's default value.
	private Integer isolation;

	private boolean autocommit;

	private ClassLoaderService classLoaderService;

	@Override
	public Connection getConnection() throws SQLException {
		// get a connection from the pool (thru DriverManager, cfr. Proxool doc)
		final Connection c = DriverManager.getConnection( proxoolAlias );

		// set the Transaction Isolation if defined
		if ( isolation != null ) {
			c.setTransactionIsolation( isolation );
		}

		// toggle autoCommit to false if set
		if ( c.getAutoCommit() != autocommit ) {
			c.setAutoCommit( autocommit );
		}

		// return the connection
		return c;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				ProxoolConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				ProxoolConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@Override
	public void configure(Map props) {
		// Get the configurator files (if available)
		final String jaxpFile = (String) props.get( Environment.PROXOOL_XML );
		final String propFile = (String) props.get( Environment.PROXOOL_PROPERTIES );
		final String externalConfig = (String) props.get( Environment.PROXOOL_EXISTING_POOL );

		// Default the Proxool alias setting
		proxoolAlias = (String) props.get( Environment.PROXOOL_POOL_ALIAS );

		// Configured outside of Hibernate (i.e. Servlet container, or Java Bean Container
		// already has Proxool pools running, and this provider is to just borrow one of these
		if ( "true".equals( externalConfig ) ) {
			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				final String msg = LOG.unableToConfigureProxoolProviderToUseExistingInMemoryPool( Environment.PROXOOL_POOL_ALIAS );
				LOG.error( msg );
				throw new HibernateException( msg );
			}
			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;

			// Set the existing pool flag to true
			existingPool = true;

			LOG.configuringProxoolProviderUsingExistingPool( proxoolAlias );

			// Configured using the JAXP Configurator
		}
		else if ( StringHelper.isNotEmpty( jaxpFile ) ) {
			LOG.configuringProxoolProviderUsingJaxpConfigurator( jaxpFile );

			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				final String msg = LOG.unableToConfigureProxoolProviderToUseJaxp( Environment.PROXOOL_POOL_ALIAS );
				LOG.error( msg );
				throw new HibernateException( msg );
			}

			try {
				JAXPConfigurator.configure( getConfigStreamReader( jaxpFile ), false );
			}
			catch (ProxoolException e) {
				final String msg = LOG.unableToLoadJaxpConfiguratorFile( jaxpFile );
				LOG.error( msg, e );
				throw new HibernateException( msg, e );
			}

			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;
			LOG.configuringProxoolProviderToUsePoolAlias( proxoolAlias );

			// Configured using the Properties File Configurator
		}
		else if ( StringHelper.isNotEmpty( propFile ) ) {
			LOG.configuringProxoolProviderUsingPropertiesFile( propFile );

			// Validate that an alias name was provided to determine which pool to use
			if ( !StringHelper.isNotEmpty( proxoolAlias ) ) {
				final String msg = LOG.unableToConfigureProxoolProviderToUsePropertiesFile( Environment.PROXOOL_POOL_ALIAS );
				LOG.error( msg );
				throw new HibernateException( msg );
			}

			try {
				PropertyConfigurator.configure( getConfigProperties( propFile ) );
			}
			catch (ProxoolException e) {
				final String msg = LOG.unableToLoadPropertyConfiguratorFile( propFile );
				LOG.error( msg, e );
				throw new HibernateException( msg, e );
			}

			// Append the stem to the proxool pool alias
			proxoolAlias = PROXOOL_JDBC_STEM + proxoolAlias;
			LOG.configuringProxoolProviderToUsePoolAlias( proxoolAlias );
		}

		// Remember Isolation level
		isolation = ConnectionProviderInitiator.extractIsolation( props );
		LOG.jdbcIsolationLevel( ConnectionProviderInitiator.toIsolationNiceName( isolation ) );

		autocommit = ConfigurationHelper.getBoolean( Environment.AUTOCOMMIT, props );
		LOG.autoCommitMode( autocommit );
	}

	private Reader getConfigStreamReader(String resource) {
		return new InputStreamReader( classLoaderService.locateResourceStream( resource ) );
	}

	private Properties getConfigProperties(String resource) {
		try {
			Properties properties = new Properties();
			properties.load( classLoaderService.locateResourceStream( resource ) );
			return properties;
		}
		catch (IOException e) {
			throw new HibernateException( "Unable to load properties from specified config file: " + resource, e );
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}


	@Override
	public void stop() {
		// If the provider was leeching off an existing pool don't close it
		if ( existingPool ) {
			return;
		}

		// We have created the pool ourselves, so shut it down
		try {
			if ( ProxoolFacade.getAliases().length == 1 ) {
				ProxoolFacade.shutdown( 0 );
			}
			else {
				ProxoolFacade.removeConnectionPool( proxoolAlias.substring( PROXOOL_JDBC_STEM.length() ) );
			}
		}
		catch (Exception e) {
			// If you're closing down the ConnectionProvider chances are an
			// is not a real big deal, just warn
			final String msg = LOG.exceptionClosingProxoolPool();
			LOG.warn( msg, e );
			throw new HibernateException( msg, e );
		}
	}

	/**
	 * Release all resources held by this provider.
	 *
	 * @throws HibernateException Indicates a problem closing the underlying pool or releasing resources
	 *
	 * @deprecated Use {@link #stop} instead
	 */
	@Deprecated
	public void close() throws HibernateException {
		stop();
	}
}
