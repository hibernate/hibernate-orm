/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.tool.hbm2ddl;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformationBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 * @author Brett Meyer
 */
public class SchemaValidator {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SchemaValidator.class.getName());

	private ConnectionHelper connectionHelper;
	private Dialect dialect;
	
	private MetadataImplementor metadata;
	// TODO: eventually remove
	private Configuration configuration;

	public SchemaValidator(MetadataImplementor metadata, Connection connection) {
		this.metadata = metadata;
		ServiceRegistry serviceRegistry = metadata.getServiceRegistry();
		if ( connection != null ) {
			this.connectionHelper = new SuppliedConnectionHelper( connection );
		}
		else {
			this.connectionHelper = new SuppliedConnectionProviderConnectionHelper(
					serviceRegistry.getService( ConnectionProvider.class )
			);
		}
	}
	
	public SchemaValidator(MetadataImplementor metadata) {
		this( metadata, null );
	}

	@Deprecated
	public SchemaValidator(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	@Deprecated
	public SchemaValidator(Configuration cfg, Properties connectionProperties) throws HibernateException {
		this.configuration = cfg;
		dialect = Dialect.getDialect( connectionProperties );
		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( connectionProperties );
		connectionHelper = new ManagedProviderConnectionHelper( props );
	}

	@Deprecated
	public SchemaValidator(ServiceRegistry serviceRegistry, Configuration cfg ) throws HibernateException {
		this.configuration = cfg;
		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.dialect = jdbcServices.getDialect();
		this.connectionHelper = new SuppliedConnectionProviderConnectionHelper( jdbcServices.getConnectionProvider() );
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings( properties ).build();
	}

	public static void main(String[] args) {
		try {
			final Configuration cfg = new Configuration();
			final StandardServiceRegistryImpl serviceRegistry = createServiceRegistry( cfg.getProperties() );
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

			String propFile = null;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[i].startsWith( "--" ) ) {
					if ( args[i].startsWith( "--properties=" ) ) {
						propFile = args[i].substring( 13 );
					}
					else if ( args[i].startsWith( "--config=" ) ) {
						cfg.configure( args[i].substring( 9 ) );
					}
					else if ( args[i].startsWith( "--naming=" ) ) {
						cfg.setNamingStrategy(
								( NamingStrategy ) classLoaderService.classForName( args[i].substring( 9 ) ).newInstance()
						);
					}
				}
				else {
					cfg.addFile( args[i] );
				}

			}

			if ( propFile != null ) {
				Properties props = new Properties();
				props.putAll( cfg.getProperties() );
				props.load( new FileInputStream( propFile ) );
				cfg.setProperties( props );
			}

			try {
				new SchemaValidator( serviceRegistry, cfg ).validate();
			}
			finally {
				serviceRegistry.destroy();
			}
		}
		catch ( Exception e ) {
            LOG.unableToRunSchemaUpdate(e);
			e.printStackTrace();
		}
	}

	/**
	 * Perform the validations.
	 */
	public void validate() {

        LOG.runningSchemaValidator();

		try {
			
			connectionHelper.prepare( false );
			Connection connection = connectionHelper.getConnection();
			
			if ( metadata != null ) {
				final ServiceRegistry serviceRegistry = metadata.getServiceRegistry();
				final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

				final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcServices.class )
						.getJdbcEnvironment();
				final DatabaseInformation databaseInformation
						= new DatabaseInformationBuilder( jdbcEnvironment, connection ).prepareAll().build();
				final SchemaManagementTool schemaManagementTool = serviceRegistry.getService(
						SchemaManagementTool.class );
				schemaManagementTool.getSchemaValidator( settings ).doValidation(
						metadata.getDatabase(), databaseInformation );
			}
			else {
				DatabaseMetadata meta;
				try {
	                LOG.fetchingDatabaseMetadata();
					meta = new DatabaseMetadata( connection, dialect, configuration, false );
				}
				catch ( SQLException sqle ) {
	                LOG.unableToGetDatabaseMetadata(sqle);
					throw sqle;
				}
				configuration.validateSchema( dialect, meta );
			}
		}
		catch ( SQLException e ) {
            LOG.unableToCompleteSchemaValidation(e);
		}
		finally {

			try {
				connectionHelper.release();
			}
			catch ( Exception e ) {
                LOG.unableToCloseConnection(e);
			}

		}
	}
}
