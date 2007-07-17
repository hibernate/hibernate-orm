//$Id: SchemaUpdate.java 9250 2006-02-10 03:48:37Z steveebersole $
package org.hibernate.tool.hbm2ddl;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.ReflectHelper;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 */
public class SchemaUpdate {

	private static final Log log = LogFactory.getLog( SchemaUpdate.class );
	private ConnectionHelper connectionHelper;
	private Configuration configuration;
	private Dialect dialect;
	private List exceptions;

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
	}

	public SchemaUpdate(Configuration cfg, Settings settings) throws HibernateException {
		this.configuration = cfg;
		dialect = settings.getDialect();
		connectionHelper = new SuppliedConnectionProviderConnectionHelper(
				settings.getConnectionProvider()
		);
		exceptions = new ArrayList();
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
			log.error( "Error running schema update", e );
			e.printStackTrace();
		}
	}

	/**
	 * Execute the schema updates
	 *
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate) {

		log.info( "Running hbm2ddl schema update" );

		Connection connection = null;
		Statement stmt = null;

		exceptions.clear();

		try {

			DatabaseMetadata meta;
			try {
				log.info( "fetching database metadata" );
				connectionHelper.prepare( true );
				connection = connectionHelper.getConnection();
				meta = new DatabaseMetadata( connection, dialect );
				stmt = connection.createStatement();
			}
			catch ( SQLException sqle ) {
				exceptions.add( sqle );
				log.error( "could not get database metadata", sqle );
				throw sqle;
			}

			log.info( "updating schema" );

			String[] createSQL = configuration.generateSchemaUpdateScript( dialect, meta );
			for ( int j = 0; j < createSQL.length; j++ ) {

				final String sql = createSQL[j];
				try {
					if ( script ) {
						System.out.println( sql );
					}
					if ( doUpdate ) {
						log.debug( sql );
						stmt.executeUpdate( sql );
					}
				}
				catch ( SQLException e ) {
					exceptions.add( e );
					log.error( "Unsuccessful: " + sql );
					log.error( e.getMessage() );
				}
			}

			log.info( "schema update complete" );

		}
		catch ( Exception e ) {
			exceptions.add( e );
			log.error( "could not complete schema update", e );
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
				log.error( "Error closing connection", e );
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
}
