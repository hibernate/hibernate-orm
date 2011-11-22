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
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;

/**
 * A commandline tool to update a database schema. May also be called from inside an application.
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public class SchemaUpdate {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SchemaUpdate.class.getName());

	private final Configuration configuration;
	private final ConnectionHelper connectionHelper;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final Dialect dialect;

	private final List<Exception> exceptions = new ArrayList<Exception>();

	private Formatter formatter;

	private boolean haltOnError = false;
	private boolean format = true;
	private String outputFile = null;
	private String delimiter;

	public SchemaUpdate(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	public SchemaUpdate(Configuration configuration, Properties properties) throws HibernateException {
		this.configuration = configuration;
		this.dialect = Dialect.getDialect( properties );

		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( properties );
		this.connectionHelper = new ManagedProviderConnectionHelper( props );

		this.sqlExceptionHelper = new SqlExceptionHelper();
		this.sqlStatementLogger = new SqlStatementLogger( false, true );
		this.formatter = FormatStyle.DDL.getFormatter();
	}

	public SchemaUpdate(ServiceRegistry serviceRegistry, Configuration cfg) throws HibernateException {
		this.configuration = cfg;

		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.dialect = jdbcServices.getDialect();
		this.connectionHelper = new SuppliedConnectionProviderConnectionHelper( jdbcServices.getConnectionProvider() );

		this.sqlExceptionHelper = new SqlExceptionHelper();
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new ServiceRegistryBuilder().applySettings( properties ).buildServiceRegistry();
	}

	public static void main(String[] args) {
		try {
			Configuration cfg = new Configuration();

			boolean script = true;
			// If true then execute db updates, otherwise just generate and display updates
			boolean doUpdate = true;
			String propFile = null;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[i].startsWith( "--" ) ) {
					if ( args[i].equals( "--quiet" ) ) {
						script = false;
					}
					else if ( args[i].startsWith( "--properties=" ) ) {
						propFile = args[i].substring( 13 );
					}
					else if ( args[i].startsWith( "--config=" ) ) {
						cfg.configure( args[i].substring( 9 ) );
					}
					else if ( args[i].startsWith( "--text" ) ) {
						doUpdate = false;
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
				new SchemaUpdate( serviceRegistry, cfg ).execute( script, doUpdate );
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
	 * Execute the schema updates
	 *
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate) {
		execute( Target.interpret( script, doUpdate ) );
	}
	
	public void execute(Target target) {
        LOG.runningHbm2ddlSchemaUpdate();

		Connection connection = null;
		Statement stmt = null;
		Writer outputFileWriter = null;

		exceptions.clear();

		try {
			DatabaseMetadata meta;
			try {
                LOG.fetchingDatabaseMetadata();
				connectionHelper.prepare( true );
				connection = connectionHelper.getConnection();
				meta = new DatabaseMetadata( connection, dialect );
				stmt = connection.createStatement();
			}
			catch ( SQLException sqle ) {
				exceptions.add( sqle );
                LOG.unableToGetDatabaseMetadata(sqle);
				throw sqle;
			}

            LOG.updatingSchema();

			if ( outputFile != null ) {
                LOG.writingGeneratedSchemaToFile( outputFile );
				outputFileWriter = new FileWriter( outputFile );
			}

			String[] sqlStrings = configuration.generateSchemaUpdateScript( dialect, meta );
			for ( String sql : sqlStrings ) {
				String formatted = formatter.format( sql );
				try {
					if ( delimiter != null ) {
						formatted += delimiter;
					}
					if ( target.doScript() ) {
						System.out.println( formatted );
					}
					if ( outputFile != null ) {
						outputFileWriter.write( formatted + "\n" );
					}
					if ( target.doExport() ) {
                        LOG.debug( sql );
						stmt.executeUpdate( formatted );
					}
				}
				catch ( SQLException e ) {
					if ( haltOnError ) {
						throw new JDBCException( "Error during DDL export", e );
					}
					exceptions.add( e );
                    LOG.unsuccessful(sql);
                    LOG.error(e.getMessage());
				}
			}

            LOG.schemaUpdateComplete();

		}
		catch ( Exception e ) {
			exceptions.add( e );
            LOG.unableToCompleteSchemaUpdate(e);
		}
		finally {

			try {
				if ( stmt != null ) {
					stmt.close();
				}
				connectionHelper.release();
			}
			catch ( Exception e ) {
				exceptions.add( e );
                LOG.unableToCloseConnection(e);
			}
			try {
				if( outputFileWriter != null ) {
					outputFileWriter.close();
				}
			}
			catch(Exception e) {
				exceptions.add(e);
                LOG.unableToCloseConnection(e);
			}
		}
	}

	/**
	 * Returns a List of all Exceptions which occured during the export.
	 *
	 * @return A List containig the Exceptions occured during the export
	 */
	public List getExceptions() {
		return exceptions;
	}

	public void setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	public void setFormat(boolean format) {
		this.formatter = ( format ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
