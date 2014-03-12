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

import java.io.BufferedReader;
import java.io.File;
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
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

/**
 * Commandline tool to export table schema to the database. This class may also be called from inside an application.
 *
 * @author Daniel Bradby
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SchemaExport {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SchemaExport.class.getName()
	);
	private static final String DEFAULT_IMPORT_FILE = "/import.sql";

	public static enum Type {
		CREATE,
		DROP,
		NONE,
		BOTH;

		public boolean doCreate() {
			return this == BOTH || this == CREATE;
		}

		public boolean doDrop() {
			return this == BOTH || this == DROP;
		}
	}

	private final ConnectionHelper connectionHelper;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final String[] dropSQL;
	private final String[] createSQL;
	private final String importFiles;
	
	private final ClassLoaderService classLoaderService;

	private final List<Exception> exceptions = new ArrayList<Exception>();

	private Formatter formatter;
	private ImportSqlCommandExtractor importSqlCommandExtractor = ImportSqlCommandExtractorInitiator.DEFAULT_EXTRACTOR;

	private String outputFile = null;
	private String delimiter;
	private boolean haltOnError = false;

//	public SchemaExport(ServiceRegistry serviceRegistry, Configuration configuration) {
//		this.connectionHelper = new SuppliedConnectionProviderConnectionHelper(
//				serviceRegistry.getService( ConnectionProvider.class )
//		);
//		this.sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();
//		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
//		this.sqlExceptionHelper = serviceRegistry.getService( JdbcServices.class ).getSqlExceptionHelper();
//
//		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
//
//		this.importFiles = ConfigurationHelper.getString(
//				AvailableSettings.HBM2DDL_IMPORT_FILES,
//				configuration.getProperties(),
//				DEFAULT_IMPORT_FILE
//		);
//
//		final Dialect dialect = serviceRegistry.getService( JdbcServices.class ).getDialect();
//		this.dropSQL = configuration.generateDropSchemaScript( dialect );
//		this.createSQL = configuration.generateSchemaCreationScript( dialect );
//	}

	public SchemaExport(MetadataImplementor metadata, Connection connection){
		ServiceRegistry serviceRegistry = metadata.getServiceRegistry();
		if ( connection != null ) {
			this.connectionHelper = new SuppliedConnectionHelper( connection );
		}
		else {
			this.connectionHelper = new SuppliedConnectionProviderConnectionHelper(
					serviceRegistry.getService( ConnectionProvider.class )
			);
		}
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
		
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

		this.importFiles = ConfigurationHelper.getString(
				AvailableSettings.HBM2DDL_IMPORT_FILES,
				settings,
				DEFAULT_IMPORT_FILE
		);

		// uses the schema management tool service to generate the create/drop scripts
		// longer term this class should instead just leverage the tool for its execution phase...

		SchemaManagementTool schemaManagementTool = serviceRegistry.getService( SchemaManagementTool.class );
		final List<String> commands = new ArrayList<String>();
		final org.hibernate.tool.schema.spi.Target target = new org.hibernate.tool.schema.spi.Target() {
			@Override
			public boolean acceptsImportScriptActions() {
				return false;
			}

			@Override
			public void prepare() {
				commands.clear();
			}

			@Override
			public void accept(String command) {
				commands.add( command );
			}

			@Override
			public void release() {
			}
		};

		schemaManagementTool.getSchemaDropper( settings ).doDrop( metadata.getDatabase(), false, target );
		this.dropSQL = commands.toArray( new String[ commands.size() ] );

		schemaManagementTool.getSchemaCreator( settings ).doCreation( metadata.getDatabase(), false, target );
		this.createSQL = commands.toArray( new String[commands.size()] );
	}
	public SchemaExport(MetadataImplementor metadata) {
		this(metadata, null);
	}

//	/**
//	 * Create a schema exporter for the given Configuration
//	 *
//	 * @param configuration The configuration from which to build a schema export.
//	 *
//	 * @throws HibernateException Indicates problem preparing for schema export.
//	 */
//	public SchemaExport(Configuration configuration) {
//		this( configuration, configuration.getProperties() );
//	}

//	/**
//	 * Create a schema exporter for the given Configuration, with the given
//	 * database connection properties.
//	 *
//	 * @param configuration The configuration from which to build a schema export.
//	 * @param properties The properties from which to configure connectivity etc.
//	 *
//	 * @throws HibernateException Indicates problem preparing for schema export.
//	 * @deprecated properties may be specified via the Configuration object
//	 */
//	@Deprecated
//	public SchemaExport(Configuration configuration, Properties properties) throws HibernateException {
//		final Dialect dialect = Dialect.getDialect( properties );
//
//		Properties props = new Properties();
//		props.putAll( dialect.getDefaultProperties() );
//		props.putAll( properties );
//		this.connectionHelper = new ManagedProviderConnectionHelper( props );
//
//		this.sqlStatementLogger = new SqlStatementLogger( false, true );
//		this.formatter = FormatStyle.DDL.getFormatter();
//		this.sqlExceptionHelper = new SqlExceptionHelper();
//
//		this.classLoaderService = new ClassLoaderServiceImpl();
//
//		this.importFiles = ConfigurationHelper.getString(
//				AvailableSettings.HBM2DDL_IMPORT_FILES,
//				properties,
//				DEFAULT_IMPORT_FILE
//		);
//
//		this.dropSQL = configuration.generateDropSchemaScript( dialect );
//		this.createSQL = configuration.generateSchemaCreationScript( dialect );
//	}

//	/**
//	 * Create a schema exporter for the given Configuration, using the supplied connection for connectivity.
//	 *
//	 * @param configuration The configuration to use.
//	 * @param connection The JDBC connection to use.
//	 *
//	 * @throws HibernateException Indicates problem preparing for schema export.
//	 */
//	public SchemaExport(Configuration configuration, Connection connection) throws HibernateException {
//		this.connectionHelper = new SuppliedConnectionHelper( connection );
//
//		this.sqlStatementLogger = new SqlStatementLogger( false, true );
//		this.formatter = FormatStyle.DDL.getFormatter();
//		this.sqlExceptionHelper = new SqlExceptionHelper();
//
//		this.classLoaderService = new ClassLoaderServiceImpl();
//
//		this.importFiles = ConfigurationHelper.getString(
//				AvailableSettings.HBM2DDL_IMPORT_FILES,
//				configuration.getProperties(),
//				DEFAULT_IMPORT_FILE
//		);
//
//		final Dialect dialect = Dialect.getDialect( configuration.getProperties() );
//		this.dropSQL = configuration.generateDropSchemaScript( dialect );
//		this.createSQL = configuration.generateSchemaCreationScript( dialect );
//	}

	public SchemaExport(
			ConnectionHelper connectionHelper,
			String[] dropSql,
			String[] createSql) {
		this.connectionHelper = connectionHelper;
		this.dropSQL = dropSql;
		this.createSQL = createSql;
		this.importFiles = "";
		this.sqlStatementLogger = new SqlStatementLogger( false, true );
		this.sqlExceptionHelper = new SqlExceptionHelper();
		this.classLoaderService = new ClassLoaderServiceImpl();
		this.formatter = FormatStyle.DDL.getFormatter();
	}

	/**
	 * For generating a export script file, this is the file which will be written.
	 *
	 * @param filename The name of the file to which to write the export script.
	 *
	 * @return this
	 */
	public SchemaExport setOutputFile(String filename) {
		outputFile = filename;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 *
	 * @param delimiter The delimiter
	 *
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
	 *
	 * @return this
	 */
	public SchemaExport setFormat(boolean format) {
		this.formatter = ( format ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
		return this;
	}

	/**
	 * Set <i>import.sql</i> command extractor. By default {@link SingleLineSqlCommandExtractor} is used.
	 *
	 * @param importSqlCommandExtractor <i>import.sql</i> command extractor.
	 *
	 * @return this
	 */
	public SchemaExport setImportSqlCommandExtractor(ImportSqlCommandExtractor importSqlCommandExtractor) {
		this.importSqlCommandExtractor = importSqlCommandExtractor;
		return this;
	}

	/**
	 * Should we stop once an error occurs?
	 *
	 * @param haltOnError True if export should stop after error.
	 *
	 * @return this
	 */
	public SchemaExport setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
		return this;
	}

	/**
	 * Run the schema creation script; drop script is automatically
	 * executed before running the creation script.
	 *
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void create(boolean script, boolean export) {
		create( Target.interpret( script, export ) );
	}

	/**
	 * Run the schema creation script; drop script is automatically
	 * executed before running the creation script.
	 *
	 * @param output the target of the script.
	 */
	public void create(Target output) {
		// need to drop tables before creating so need to specify Type.BOTH
		execute( output, Type.BOTH );
	}

	/**
	 * Run the drop schema script.
	 *
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void drop(boolean script, boolean export) {
		drop( Target.interpret( script, export ) );
	}

	public void drop(Target output) {
		execute( output, Type.DROP );
	}

	public void execute(boolean script, boolean export, boolean justDrop, boolean justCreate) {
		execute( Target.interpret( script, export ), interpretType( justDrop, justCreate ) );
	}

	private Type interpretType(boolean justDrop, boolean justCreate) {
		if ( justDrop ) {
			return Type.DROP;
		}
		else if ( justCreate ) {
			return Type.CREATE;
		}
		else {
			return Type.BOTH;
		}
	}

	public void execute(Target output, Type type) {
		if ( (outputFile == null && output == Target.NONE) || type == SchemaExport.Type.NONE ) {
			return;
		}
		exceptions.clear();

		LOG.runningHbm2ddlSchemaExport();

		final List<NamedReader> importFileReaders = new ArrayList<NamedReader>();
		for ( String currentFile : importFiles.split( "," ) ) {
			try {
				final String resourceName = currentFile.trim();
				InputStream stream = classLoaderService.locateResourceStream( resourceName );
				if ( stream != null ) {
					importFileReaders.add( new NamedReader( resourceName, stream ) );
				}
				else {
					LOG.debugf( "Import file not found: %s", currentFile );
				}
			}
			catch ( HibernateException e ) {
				LOG.debugf( "Import file not found: %s", currentFile );
			}
		}

		final List<Exporter> exporters = new ArrayList<Exporter>();
		try {
			// prepare exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if ( output.doScript() ) {
				exporters.add( new ScriptExporter() );
			}
			if ( outputFile != null ) {
				exporters.add( new FileExporter( outputFile ) );
			}
			if ( output.doExport() ) {
				exporters.add( new DatabaseExporter( connectionHelper, sqlExceptionHelper ) );
			}

			// perform exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if ( type.doDrop() ) {
				perform( dropSQL, exporters );
			}
			if ( type.doCreate() ) {
				perform( createSQL, exporters );
				if ( !importFileReaders.isEmpty() ) {
					for ( NamedReader namedReader : importFileReaders ) {
						importScript( namedReader, exporters );
					}
				}
			}
		}
		catch ( Exception e ) {
			exceptions.add( e );
			LOG.schemaExportUnsuccessful( e );
		}
		finally {
			// release exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			for ( Exporter exporter : exporters ) {
				try {
					exporter.release();
				}
				catch ( Exception ignore ) {
				}
			}

			// release the named readers from import scripts
			for ( NamedReader namedReader : importFileReaders ) {
				try {
					namedReader.getReader().close();
				}
				catch ( Exception ignore ) {
				}
			}
			LOG.schemaExportComplete();
		}
	}

	private void perform(String[] sqlCommands, List<Exporter> exporters) {
		for ( String sqlCommand : sqlCommands ) {
			String formatted = formatter.format( sqlCommand );
			if ( delimiter != null ) {
				formatted += delimiter;
			}
			sqlStatementLogger.logStatement( sqlCommand, formatter );
			for ( Exporter exporter : exporters ) {
				try {
					exporter.export( formatted );
				}
				catch ( Exception e ) {
					if ( haltOnError ) {
						throw new HibernateException( "Error during DDL export", e );
					}
					exceptions.add( e );
					LOG.unsuccessfulCreate( sqlCommand );
					LOG.error( e.getMessage() );
				}
			}
		}
	}

	private void importScript(NamedReader namedReader, List<Exporter> exporters) throws Exception {
		BufferedReader reader = new BufferedReader( namedReader.getReader() );
		String[] statements = importSqlCommandExtractor.extractCommands( reader );
		if ( statements != null ) {
			for ( String statement : statements ) {
				if ( statement != null ) {
					String trimmedSql = statement.trim();
					if ( trimmedSql.endsWith( ";" ) ) {
						trimmedSql = trimmedSql.substring( 0, statement.length() - 1 );
					}
					if ( !StringHelper.isEmpty( trimmedSql ) ) {
						try {
							for ( Exporter exporter : exporters ) {
								if ( exporter.acceptsImportScripts() ) {
									exporter.export( trimmedSql );
								}
							}
						}
						catch ( Exception e ) {
						  	if (haltOnError) {
						  		throw new ImportScriptException( "Error during statement execution (file: '"
						  				+ namedReader.getName() + "'): " + trimmedSql, e );
							}
						  	exceptions.add(e);
						  	LOG.unsuccessful(trimmedSql);
						  	LOG.error(e.getMessage());
						}
					}
				}
			}
		}
	}

	private static class NamedReader {
		private final Reader reader;
		private final String name;

		public NamedReader(String name, InputStream stream) {
			this.name = name;
			this.reader = new InputStreamReader( stream );
		}

		public Reader getReader() {
			return reader;
		}

		public String getName() {
			return name;
		}
	}

	private void execute(boolean script, boolean export, Writer fileOutput, Statement statement, final String sql)
			throws IOException, SQLException {
		final SqlExceptionHelper sqlExceptionHelper = new SqlExceptionHelper();

		String formatted = formatter.format( sql );
		if ( delimiter != null ) {
			formatted += delimiter;
		}
		if ( script ) {
			System.out.println( formatted );
		}
		LOG.debug( formatted );
		if ( outputFile != null ) {
			fileOutput.write( formatted + "\n" );
		}
		if ( export ) {

			statement.executeUpdate( sql );
			try {
				SQLWarning warnings = statement.getWarnings();
				if ( warnings != null ) {
					sqlExceptionHelper.logAndClearWarnings( connectionHelper.getConnection() );
				}
			}
			catch ( SQLException sqle ) {
				LOG.unableToLogSqlWarnings( sqle );
			}
		}

	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings( properties ).build();
	}

	public static void main(String[] args) {
		try {
			final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
			final ClassLoaderService classLoaderService = bsr.getService( ClassLoaderService.class );

			final MetadataSources metadataSources = new MetadataSources( bsr );
			final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

			NamingStrategy namingStrategy = null;

			boolean script = true;
			boolean drop = false;
			boolean create = false;
			boolean halt = false;
			boolean export = true;
			String outFile = null;
			String importFile = DEFAULT_IMPORT_FILE;
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
						ssrBuilder.loadProperties( new File( args[i].substring( 13 ) ) );
					}
					else if ( args[i].equals( "--format" ) ) {
						format = true;
					}
					else if ( args[i].startsWith( "--delimiter=" ) ) {
						delim = args[i].substring( 12 );
					}
					else if ( args[i].startsWith( "--config=" ) ) {
						ssrBuilder.configure( args[i].substring( 9 ) );
					}
					else if ( args[i].startsWith( "--naming=" ) ) {
						namingStrategy = (NamingStrategy) classLoaderService.classForName( args[i].substring( 9 ) ).newInstance();
					}
				}
				else {
					String filename = args[i];
					if ( filename.endsWith( ".jar" ) ) {
						metadataSources.addJar( new File( filename ) );
					}
					else {
						metadataSources.addFile( filename );
					}
				}
			}

			if ( importFile != null ) {
				ssrBuilder.applySetting( AvailableSettings.HBM2DDL_IMPORT_FILES, importFile );
			}

			final StandardServiceRegistryImpl ssr = (StandardServiceRegistryImpl) ssrBuilder.build();
			final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder( ssr );
			if ( namingStrategy != null ) {
				metadataBuilder.with( namingStrategy );
			}

			final MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();

			try {
				SchemaExport se = new SchemaExport( metadata )
						.setHaltOnError( halt )
						.setOutputFile( outFile )
						.setDelimiter( delim )
						.setImportSqlCommandExtractor( ssr.getService( ImportSqlCommandExtractor.class ) );
				if ( format ) {
					se.setFormat( true );
				}
				se.execute( script, export, drop, create );
			}
			finally {
				ssr.destroy();
			}
		}
		catch ( Exception e ) {
			LOG.unableToCreateSchema( e );
			e.printStackTrace();
		}
	}

	/**
	 * Returns a List of all Exceptions which occurred during the export.
	 *
	 * @return A List containing the Exceptions occurred during the export
	 */
	public List getExceptions() {
		return exceptions;
	}
	
	public String[] getCreateSQL() {
		return createSQL;
	}
	
	public String[] getDropSQL() {
		return dropSQL;
	}

}
