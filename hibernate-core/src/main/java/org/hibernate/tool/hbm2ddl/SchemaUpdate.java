/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.tool.hbm2ddl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.jdbc.util.Formatter;
import org.hibernate.util.ReflectHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 */
public class SchemaUpdate {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                SchemaUpdate.class.getPackage().getName());
	private ConnectionHelper connectionHelper;
	private Configuration configuration;
	private Dialect dialect;
	private List exceptions;
	private boolean haltOnError = false;
	private boolean format = true;
	private String outputFile = null;
	private String delimiter;
	private Formatter formatter;
	private SQLStatementLogger sqlStatementLogger;

	public SchemaUpdate(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	public SchemaUpdate(Configuration cfg, Properties connectionProperties) throws HibernateException {
		this.configuration = cfg;
		dialect = Dialect.getDialect( connectionProperties );
		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( connectionProperties );
		connectionHelper = new ManagedProviderConnectionHelper( props );
		exceptions = new ArrayList();
		formatter = ( ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, props ) ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	public SchemaUpdate(JdbcServices jdbcServices, Configuration cfg) throws HibernateException {
		this.configuration = cfg;
		dialect = jdbcServices.getDialect();
		connectionHelper = new SuppliedConnectionProviderConnectionHelper(
				jdbcServices.getConnectionProvider()
		);
		exceptions = new ArrayList();
		sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
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

			new SchemaUpdate( cfg ).execute( script, doUpdate );
		}
		catch ( Exception e ) {
            LOG.error(LOG.unableToRunSchemaUpdate(), e);
			e.printStackTrace();
		}
	}

	/**
	 * Execute the schema updates
	 *
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate) {

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
                LOG.error(LOG.unableToGetDatabaseMetadata(), sqle);
				throw sqle;
			}

            LOG.updatingSchema();


			if ( outputFile != null ) {
                LOG.writingGeneratedSchemaToFile(outputFile);
				outputFileWriter = new FileWriter( outputFile );
			}

			String[] createSQL = configuration.generateSchemaUpdateScript( dialect, meta );
			for ( int j = 0; j < createSQL.length; j++ ) {

				final String sql = createSQL[j];
				String formatted = formatter.format( sql );
				try {
					if ( delimiter != null ) {
						formatted += delimiter;
					}
					if ( script ) {
						System.out.println( formatted );
					}
					if ( outputFile != null ) {
						outputFileWriter.write( formatted + "\n" );
					}
					if ( doUpdate ) {
                        LOG.debug(sql);
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
            LOG.error(LOG.unableToCompleteSchemaUpdate(), e);
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
                LOG.error(LOG.unableToCloseConnection(), e);
			}
			try {
				if( outputFileWriter != null ) {
					outputFileWriter.close();
				}
			}
			catch(Exception e) {
				exceptions.add(e);
                LOG.error(LOG.unableToCloseConnection(), e);
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

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = INFO )
        @Message( value = "Fetching database metadata" )
        void fetchingDatabaseMetadata();

        @LogMessage( level = INFO )
        @Message( value = "Running hbm2ddl schema update" )
        void runningHbm2ddlSchemaUpdate();

        @LogMessage( level = INFO )
        @Message( value = "Schema update complete" )
        void schemaUpdateComplete();

        @Message( value = "Error closing connection" )
        Object unableToCloseConnection();

        @Message( value = "Could not complete schema update" )
        Object unableToCompleteSchemaUpdate();

        @Message( value = "Could not get database metadata" )
        Object unableToGetDatabaseMetadata();

        @Message( value = "Error running schema update" )
        Object unableToRunSchemaUpdate();

        @LogMessage( level = ERROR )
        @Message( value = "Unsuccessful: %s" )
        void unsuccessful( String sql );

        @LogMessage( level = INFO )
        @Message( value = "Updating schema" )
        void updatingSchema();

        @LogMessage( level = INFO )
        @Message( value = "Writing generated schema to file: %s" )
        void writingGeneratedSchemaToFile( String outputFile );
    }
}
