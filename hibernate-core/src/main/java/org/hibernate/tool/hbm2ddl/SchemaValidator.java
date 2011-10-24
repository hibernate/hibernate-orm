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
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 */
public class SchemaValidator {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SchemaValidator.class.getName());

	private ConnectionHelper connectionHelper;
	private Configuration configuration;
	private Dialect dialect;

	public SchemaValidator(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	public SchemaValidator(Configuration cfg, Properties connectionProperties) throws HibernateException {
		this.configuration = cfg;
		dialect = Dialect.getDialect( connectionProperties );
		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( connectionProperties );
		connectionHelper = new ManagedProviderConnectionHelper( props );
	}

	public SchemaValidator(ServiceRegistry serviceRegistry, Configuration cfg ) throws HibernateException {
		this.configuration = cfg;
		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.dialect = jdbcServices.getDialect();
		this.connectionHelper = new SuppliedConnectionProviderConnectionHelper( jdbcServices.getConnectionProvider() );
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new ServiceRegistryBuilder().applySettings( properties ).buildServiceRegistry();
	}

	public static void main(String[] args) {
		try {
			Configuration cfg = new Configuration();

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
								( NamingStrategy ) ReflectHelper.classForName( args[i].substring( 9 ) ).newInstance()
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

			StandardServiceRegistryImpl serviceRegistry = createServiceRegistry( cfg.getProperties() );
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

		Connection connection = null;

		try {

			DatabaseMetadata meta;
			try {
                LOG.fetchingDatabaseMetadata();
				connectionHelper.prepare( false );
				connection = connectionHelper.getConnection();
				meta = new DatabaseMetadata( connection, dialect, false );
			}
			catch ( SQLException sqle ) {
                LOG.unableToGetDatabaseMetadata(sqle);
				throw sqle;
			}

			configuration.validateSchema( dialect, meta );

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
