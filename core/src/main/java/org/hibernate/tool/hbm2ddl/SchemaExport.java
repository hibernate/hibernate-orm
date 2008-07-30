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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.jdbc.util.Formatter;
import org.hibernate.jdbc.util.SQLStatementLogger;
import org.hibernate.util.ConfigHelper;
import org.hibernate.util.JDBCExceptionReporter;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;

/**
 * Commandline tool to export table schema to the database. This class may also be called from inside an application.
 *
 * @author Daniel Bradby
 * @author Gavin King
 */
public class SchemaExport {

	private static final Logger log = LoggerFactory.getLogger( SchemaExport.class );

	private ConnectionHelper connectionHelper;
	private String[] dropSQL;
	private String[] createSQL;
	private String outputFile = null;
	private String importFile = "/import.sql";
	private Dialect dialect;
	private String delimiter;
	private final List exceptions = new ArrayList();
	private boolean haltOnError = false;
	private Formatter formatter;
	private SQLStatementLogger sqlStatementLogger;

	/**
	 * Create a schema exporter for the given Configuration
	 *
	 * @param cfg The configuration from which to build a schema export.
	 * @throws HibernateException Indicates problem preparing for schema export.
	 */
	public SchemaExport(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	/**
	 * Create a schema exporter for the given Configuration and given settings
	 *
	 * @param cfg The configuration from which to build a schema export.
	 * @param settings The 'parsed' settings.
	 * @throws HibernateException Indicates problem preparing for schema export.
	 */
	public SchemaExport(Configuration cfg, Settings settings) throws HibernateException {
		dialect = settings.getDialect();
		connectionHelper = new SuppliedConnectionProviderConnectionHelper( settings.getConnectionProvider() );
		dropSQL = cfg.generateDropSchemaScript( dialect );
		createSQL = cfg.generateSchemaCreationScript( dialect );
		sqlStatementLogger = settings.getSqlStatementLogger();
		formatter = ( sqlStatementLogger.isFormatSql() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	/**
	 * Create a schema exporter for the given Configuration, with the given
	 * database connection properties.
	 *
	 * @param cfg The configuration from which to build a schema export.
	 * @param properties The properties from which to configure connectivity etc.
	 * @throws HibernateException Indicates problem preparing for schema export.
	 *
	 * @deprecated properties may be specified via the Configuration object
	 */
	public SchemaExport(Configuration cfg, Properties properties) throws HibernateException {
		dialect = Dialect.getDialect( properties );

		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( properties );

		connectionHelper = new ManagedProviderConnectionHelper( props );
		dropSQL = cfg.generateDropSchemaScript( dialect );
		createSQL = cfg.generateSchemaCreationScript( dialect );

		formatter = ( PropertiesHelper.getBoolean( Environment.FORMAT_SQL, props ) ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	/**
	 * Create a schema exporter for the given Configuration, using the supplied connection for connectivity.
	 *
	 * @param cfg The configuration to use.
	 * @param connection The JDBC connection to use.
	 * @throws HibernateException Indicates problem preparing for schema export.
	 */
	public SchemaExport(Configuration cfg, Connection connection) throws HibernateException {
		this.connectionHelper = new SuppliedConnectionHelper( connection );
		dialect = Dialect.getDialect( cfg.getProperties() );
		dropSQL = cfg.generateDropSchemaScript( dialect );
		createSQL = cfg.generateSchemaCreationScript( dialect );
		formatter = ( PropertiesHelper.getBoolean( Environment.FORMAT_SQL, cfg.getProperties() ) ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	/**
	 * For generating a export script file, this is the file which will be written.
	 *
	 * @param filename The name of the file to which to write the export script.
	 * @return this
	 */
	public SchemaExport setOutputFile(String filename) {
		outputFile = filename;
		return this;
	}

	/**
	 * An import file, containing raw SQL statements to be executed.
	 *
	 * @param filename The import file name.
	 * @return this
	 */
	public SchemaExport setImportFile(String filename) {
		importFile = filename;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 *
	 * @param delimiter The delimiter
	 * @return this
	 */
	public SchemaExport setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Should we format the sql strings?
	 *
	 * @param format Should we format SQL strings
	 * @return this
	 */
	public SchemaExport setFormat(boolean format) {
		this.formatter = ( format ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
		return this;
	}

	/**
	 * Should we stop once an error occurs?
	 *
	 * @param haltOnError True if export should stop after error.
	 * @return this
	 */
	public SchemaExport setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
		return this;
	}

	/**
	 * Run the schema creation script.
	 *
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void create(boolean script, boolean export) {
		execute( script, export, false, false );
	}

	/**
	 * Run the drop schema script.
	 *
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void drop(boolean script, boolean export) {
		execute( script, export, true, false );
	}

	public void execute(boolean script, boolean export, boolean justDrop, boolean justCreate) {

		log.info( "Running hbm2ddl schema export" );

		Connection connection = null;
		Writer outputFileWriter = null;
		Reader importFileReader = null;
		Statement statement = null;

		exceptions.clear();

		try {

			try {
				InputStream stream = ConfigHelper.getResourceAsStream( importFile );
				importFileReader = new InputStreamReader( stream );
			}
			catch ( HibernateException e ) {
				log.debug( "import file not found: " + importFile );
			}

			if ( outputFile != null ) {
				log.info( "writing generated schema to file: " + outputFile );
				outputFileWriter = new FileWriter( outputFile );
			}

			if ( export ) {
				log.info( "exporting generated schema to database" );
				connectionHelper.prepare( true );
				connection = connectionHelper.getConnection();
				statement = connection.createStatement();
			}

			if ( !justCreate ) {
				drop( script, export, outputFileWriter, statement );
			}

			if ( !justDrop ) {
				create( script, export, outputFileWriter, statement );
				if ( export && importFileReader != null ) {
					importScript( importFileReader, statement );
				}
			}

			log.info( "schema export complete" );

		}

		catch ( Exception e ) {
			exceptions.add( e );
			log.error( "schema export unsuccessful", e );
		}

		finally {

			try {
				if ( statement != null ) {
					statement.close();
				}
				if ( connection != null ) {
					connectionHelper.release();
				}
			}
			catch ( Exception e ) {
				exceptions.add( e );
				log.error( "Could not close connection", e );
			}

			try {
				if ( outputFileWriter != null ) {
					outputFileWriter.close();
				}
				if ( importFileReader != null ) {
					importFileReader.close();
				}
			}
			catch ( IOException ioe ) {
				exceptions.add( ioe );
				log.error( "Error closing output file: " + outputFile, ioe );
			}

		}
	}

	private void importScript(Reader importFileReader, Statement statement)
			throws IOException {
		log.info( "Executing import script: " + importFile );
		BufferedReader reader = new BufferedReader( importFileReader );
		long lineNo = 0;
		for ( String sql = reader.readLine(); sql != null; sql = reader.readLine() ) {
			try {
				lineNo++;
				String trimmedSql = sql.trim();
				if ( trimmedSql.length() == 0 ||
				     trimmedSql.startsWith( "--" ) ||
				     trimmedSql.startsWith( "//" ) ||
				     trimmedSql.startsWith( "/*" ) ) {
					continue;
				}
				else {
					if ( trimmedSql.endsWith( ";" ) ) {
						trimmedSql = trimmedSql.substring( 0, trimmedSql.length() - 1 );
					}
					log.debug( trimmedSql );
					statement.execute( trimmedSql );
				}
			}
			catch ( SQLException e ) {
				throw new JDBCException( "Error during import script execution at line " + lineNo, e );
			}
		}
	}

	private void create(boolean script, boolean export, Writer fileOutput, Statement statement)
			throws IOException {
		for ( int j = 0; j < createSQL.length; j++ ) {
			try {
				execute( script, export, fileOutput, statement, createSQL[j] );
			}
			catch ( SQLException e ) {
				if ( haltOnError ) {
					throw new JDBCException( "Error during DDL export", e );
				}
				exceptions.add( e );
				log.error( "Unsuccessful: " + createSQL[j] );
				log.error( e.getMessage() );
			}
		}
	}

	private void drop(boolean script, boolean export, Writer fileOutput, Statement statement)
			throws IOException {
		for ( int i = 0; i < dropSQL.length; i++ ) {
			try {
				execute( script, export, fileOutput, statement, dropSQL[i] );
			}
			catch ( SQLException e ) {
				exceptions.add( e );
				log.debug( "Unsuccessful: " + dropSQL[i] );
				log.debug( e.getMessage() );
			}
		}
	}

	private void execute(boolean script, boolean export, Writer fileOutput, Statement statement, final String sql)
			throws IOException, SQLException {
		String formatted = formatter.format( sql );
		if ( delimiter != null ) {
			formatted += delimiter;
		}
		if ( script ) {
			System.out.println( formatted );
		}
		log.debug( formatted );
		if ( outputFile != null ) {
			fileOutput.write( formatted + "\n" );
		}
		if ( export ) {

			statement.executeUpdate( sql );
			try {
				SQLWarning warnings = statement.getWarnings();
				if ( warnings != null) {
					JDBCExceptionReporter.logAndClearWarnings( connectionHelper.getConnection() );
				}
			}
			catch( SQLException sqle ) {
				log.warn( "unable to log SQLWarnings : " + sqle );
			}
		}

		
	}

	public static void main(String[] args) {
		try {
			Configuration cfg = new Configuration();

			boolean script = true;
			boolean drop = false;
			boolean create = false;
			boolean halt = false;
			boolean export = true;
			String outFile = null;
			String importFile = "/import.sql";
			String propFile = null;
			boolean format = false;
			String delim = null;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[i].startsWith( "--" ) ) {
					if ( args[i].equals( "--quiet" ) ) {
						script = false;
					}
					else if ( args[i].equals( "--drop" ) ) {
						drop = true;
					}
					else if ( args[i].equals( "--create" ) ) {
						create = true;
					}
					else if ( args[i].equals( "--haltonerror" ) ) {
						halt = true;
					}
					else if ( args[i].equals( "--text" ) ) {
						export = false;
					}
					else if ( args[i].startsWith( "--output=" ) ) {
						outFile = args[i].substring( 9 );
					}
					else if ( args[i].startsWith( "--import=" ) ) {
						importFile = args[i].substring( 9 );
					}
					else if ( args[i].startsWith( "--properties=" ) ) {
						propFile = args[i].substring( 13 );
					}
					else if ( args[i].equals( "--format" ) ) {
						format = true;
					}
					else if ( args[i].startsWith( "--delimiter=" ) ) {
						delim = args[i].substring( 12 );
					}
					else if ( args[i].startsWith( "--config=" ) ) {
						cfg.configure( args[i].substring( 9 ) );
					}
					else if ( args[i].startsWith( "--naming=" ) ) {
						cfg.setNamingStrategy(
								( NamingStrategy ) ReflectHelper.classForName( args[i].substring( 9 ) )
										.newInstance()
						);
					}
				}
				else {
					String filename = args[i];
					if ( filename.endsWith( ".jar" ) ) {
						cfg.addJar( new File( filename ) );
					}
					else {
						cfg.addFile( filename );
					}
				}

			}

			if ( propFile != null ) {
				Properties props = new Properties();
				props.putAll( cfg.getProperties() );
				props.load( new FileInputStream( propFile ) );
				cfg.setProperties( props );
			}

			SchemaExport se = new SchemaExport( cfg )
					.setHaltOnError( halt )
					.setOutputFile( outFile )
					.setImportFile( importFile )
					.setDelimiter( delim );
			if ( format ) {
				se.setFormat( true );
			}
			se.execute( script, export, drop, create );

		}
		catch ( Exception e ) {
			log.error( "Error creating schema ", e );
			e.printStackTrace();
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
}
