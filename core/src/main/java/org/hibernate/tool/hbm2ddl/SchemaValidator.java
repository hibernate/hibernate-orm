//$Id: SchemaValidator.java 9249 2006-02-10 03:48:37Z steveebersole $
package org.hibernate.tool.hbm2ddl;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
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
public class SchemaValidator {

	private static final Log log = LogFactory.getLog( SchemaValidator.class );
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

	public SchemaValidator(Configuration cfg, Settings settings) throws HibernateException {
		this.configuration = cfg;
		dialect = settings.getDialect();
		connectionHelper = new SuppliedConnectionProviderConnectionHelper(
				settings.getConnectionProvider()
		);
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

			new SchemaValidator( cfg ).validate();
		}
		catch ( Exception e ) {
			log.error( "Error running schema update", e );
			e.printStackTrace();
		}
	}

	/**
	 * Perform the validations.
	 */
	public void validate() {

		log.info( "Running schema validator" );

		Connection connection = null;

		try {

			DatabaseMetadata meta;
			try {
				log.info( "fetching database metadata" );
				connectionHelper.prepare( false );
				connection = connectionHelper.getConnection();
				meta = new DatabaseMetadata( connection, dialect, false );
			}
			catch ( SQLException sqle ) {
				log.error( "could not get database metadata", sqle );
				throw sqle;
			}

			configuration.validateSchema( dialect, meta );

		}
		catch ( SQLException e ) {
			log.error( "could not complete schema validation", e );
		}
		finally {

			try {
				connectionHelper.release();
			}
			catch ( Exception e ) {
				log.error( "Error closing connection", e );
			}

		}
	}

}
