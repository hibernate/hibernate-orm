/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * Commandline tool to export table schema to the database. This class may also be called from inside an application.
 *
 * @author Daniel Bradby
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SchemaExport {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaExport.class );

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
	private final ClassLoaderService classLoaderService;
	private final String[] dropSQL;
	private final String[] createSQL;
	private final String importFiles;

	private final List<Exception> exceptions = new ArrayList<Exception>();

	private Formatter formatter;
	private ImportSqlCommandExtractor importSqlCommandExtractor = ImportSqlCommandExtractorInitiator.DEFAULT_EXTRACTOR;

	private String outputFile;
	private String delimiter;
	private boolean haltOnError;

	/**
	 * Builds a SchemaExport object.
	 *
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public SchemaExport(MetadataImplementor metadata) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata );
	}

	/**
	 * Builds a SchemaExport object.
	 *
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public SchemaExport(MetadataImplementor metadata, boolean createNamespaces) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata, createNamespaces );
	}

	/**
	 * Builds a SchemaExport object.
	 *
	 * @param serviceRegistry The registry of services available for use.  Should, at a minimum, contain
	 * the JdbcServices service.
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public SchemaExport(ServiceRegistry serviceRegistry, MetadataImplementor metadata) {
		this(
				new SuppliedConnectionProviderConnectionHelper(
						serviceRegistry.getService( ConnectionProvider.class )
				),
				serviceRegistry,
				metadata,
				false
		);
	}

	/**
	 * Builds a SchemaExport object.
	 *
	 * @param serviceRegistry The registry of services available for use.  Should, at a minimum, contain
	 * the JdbcServices service.
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public SchemaExport(ServiceRegistry serviceRegistry, MetadataImplementor metadata, boolean createNamespaces) {
		this(
				new SuppliedConnectionProviderConnectionHelper(
						serviceRegistry.getService( ConnectionProvider.class )
				),
				serviceRegistry,
				metadata,
				createNamespaces
		);
	}

	private SchemaExport(
			ConnectionHelper connectionHelper,
			ServiceRegistry serviceRegistry,
			MetadataImplementor metadata,
			boolean createNamespaces) {
		this.connectionHelper = connectionHelper;
		this.sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
		this.sqlExceptionHelper = serviceRegistry.getService( JdbcEnvironment.class ).getSqlExceptionHelper();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		this.importFiles = ConfigurationHelper.getString(
				AvailableSettings.HBM2DDL_IMPORT_FILES,
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				DEFAULT_IMPORT_FILE
		);

		// uses the schema management tool service to generate the create/drop scripts
		// longer term this class should instead just leverage the tool for its execution phase.
		// That is part of the larger task to consolidate Hibernate and JPA schema management

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

		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

		schemaManagementTool.getSchemaDropper( settings ).doDrop( metadata, createNamespaces, target );
		this.dropSQL = commands.toArray( new String[commands.size()] );

		schemaManagementTool.getSchemaCreator( settings ).doCreation( metadata, createNamespaces, target );
		this.createSQL = commands.toArray( new String[commands.size()] );
	}

	/**
	 * Intended for testing use
	 *
	 * @param connectionHelper Access to the JDBC Connection
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public SchemaExport(
			ConnectionHelper connectionHelper,
			MetadataImplementor metadata) {
		this(
				connectionHelper,
				metadata.getMetadataBuildingOptions().getServiceRegistry(),
				metadata,
				false
		);
	}

	/**
	 * Create a SchemaExport for the given Metadata, using the supplied connection for connectivity.
	 *
	 * @param metadata The metadata object holding the mapping info to be exported
	 * @param connection The JDBC connection to use.
	 *
	 * @throws HibernateException Indicates problem preparing for schema export.
	 */
	public SchemaExport(MetadataImplementor metadata, Connection connection) throws HibernateException {
		this( new SuppliedConnectionHelper( connection ), metadata );
	}

	/**
	 * @deprecated Use one of the forms accepting {@link MetadataImplementor}, rather
	 * than {@link Configuration}, instead.
	 */
	@Deprecated
	public SchemaExport(ServiceRegistry serviceRegistry, Configuration configuration) {
		throw new UnsupportedOperationException(
				"Attempt to use unsupported SchemaExport constructor accepting org.hibernate.cfg.Configuration; " +
						"one of the forms accepting org.hibernate.boot.spi.MetadataImplementor should be used instead"
		);
	}

	/**
	 * @deprecated Use one of the forms accepting {@link MetadataImplementor}, rather
	 * than {@link Configuration}, instead.
	 */
	@Deprecated
	public SchemaExport(Configuration configuration) {
		throw new UnsupportedOperationException(
				"Attempt to use unsupported SchemaExport constructor accepting org.hibernate.cfg.Configuration; " +
						"one of the forms accepting org.hibernate.boot.spi.MetadataImplementor should be used instead"
		);
	}

	/**
	 * @deprecated Use one of the forms accepting {@link MetadataImplementor}, rather
	 * than {@link Configuration}, instead.
	 */
	@Deprecated
	public SchemaExport(Configuration configuration, Connection connection) throws HibernateException {
		throw new UnsupportedOperationException(
				"Attempt to use unsupported SchemaExport constructor accepting org.hibernate.cfg.Configuration; " +
						"one of the forms accepting org.hibernate.boot.spi.MetadataImplementor should be used instead"
		);
	}

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
		if ( ( outputFile == null && output == Target.NONE ) || type == SchemaExport.Type.NONE ) {
			return;
		}
		exceptions.clear();

		LOG.runningHbm2ddlSchemaExport();

		final List<NamedReader> importFileReaders = new ArrayList<NamedReader>();
		for ( String currentFile : importFiles.split( "," ) ) {
			final String resourceName = currentFile.trim();

			InputStream stream = classLoaderService.locateResourceStream( resourceName );
			if ( stream == null ) {
				LOG.debugf( "Import file not found: %s", currentFile );
			}
			else {
				importFileReaders.add( new NamedReader( resourceName, stream ) );
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
		catch (Exception e) {
			exceptions.add( e );
			LOG.schemaExportUnsuccessful( e );
		}
		finally {
			// release exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			for ( Exporter exporter : exporters ) {
				try {
					exporter.release();
				}
				catch (Exception ignore) {
				}
			}

			// release the named readers from import scripts
			for ( NamedReader namedReader : importFileReaders ) {
				try {
					namedReader.getReader().close();
				}
				catch (Exception ignore) {
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
				catch (Exception e) {
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
						catch (Exception e) {
							if ( haltOnError ) {
								throw new ImportScriptException(
										"Error during statement execution (file: '"
												+ namedReader.getName() + "'): " + trimmedSql, e
								);
							}
							exceptions.add( e );
							LOG.unsuccessful( trimmedSql );
							LOG.error( e.getMessage() );
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

	public static void main(String[] args) {
		try {
			final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
			StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
			try {
				final MetadataImplementor metadata = buildMetadata( commandLineArgs, serviceRegistry );

				SchemaExport schemaExport = new SchemaExport( serviceRegistry, metadata, commandLineArgs.exportSchemas )
						.setHaltOnError( commandLineArgs.halt )
						.setOutputFile( commandLineArgs.outputFile )
						.setDelimiter( commandLineArgs.delimiter )
						.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) )
						.setFormat( commandLineArgs.format );
				schemaExport.execute(
						commandLineArgs.script,
						commandLineArgs.export,
						commandLineArgs.drop,
						commandLineArgs.create
				);
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
		catch (Exception e) {
			LOG.unableToCreateSchema( e );
			e.printStackTrace();
		}
	}

	private static StandardServiceRegistry buildStandardServiceRegistry(CommandLineArgs commandLineArgs)
			throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( commandLineArgs.cfgXmlFile != null ) {
			ssrBuilder.configure( commandLineArgs.cfgXmlFile );
		}

		Properties properties = new Properties();
		if ( commandLineArgs.propertiesFile != null ) {
			properties.load( new FileInputStream( commandLineArgs.propertiesFile ) );
		}
		ssrBuilder.applySettings( properties );

		if ( commandLineArgs.importFile != null ) {
			ssrBuilder.applySetting( AvailableSettings.HBM2DDL_IMPORT_FILES, commandLineArgs.importFile );
		}

		return ssrBuilder.build();
	}

	private static MetadataImplementor buildMetadata(
			CommandLineArgs parsedArgs,
			StandardServiceRegistry serviceRegistry) throws Exception {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );

		for ( String filename : parsedArgs.hbmXmlFiles ) {
			metadataSources.addFile( filename );
		}

		for ( String filename : parsedArgs.jarFiles ) {
			metadataSources.addJar( new File( filename ) );
		}


		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		if ( parsedArgs.implicitNamingStrategyImplName != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					strategySelector.resolveStrategy(
							ImplicitNamingStrategy.class,
							parsedArgs.implicitNamingStrategyImplName
					)
			);
		}
		if ( parsedArgs.physicalNamingStrategyImplName != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					strategySelector.resolveStrategy(
							PhysicalNamingStrategy.class,
							parsedArgs.physicalNamingStrategyImplName
					)
			);
		}

		return (MetadataImplementor) metadataBuilder.build();
	}

	/**
	 * Intended for test usage only.  Builds a Metadata using the same algorithm  as
	 * {@link #main}
	 *
	 * @param args The "command line args"
	 *
	 * @return The built Metadata
	 *
	 * @throws Exception Problems building the Metadata
	 */
	public static MetadataImplementor buildMetadataFromMainArgs(String[] args) throws Exception {
		final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
		StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
		try {
			return buildMetadata( commandLineArgs, serviceRegistry );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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

	private static class CommandLineArgs {
		boolean script = true;
		boolean drop = false;
		boolean create = false;
		boolean halt = false;
		boolean export = true;
		boolean format = false;

		boolean exportSchemas = false;

		String delimiter = null;

		String outputFile = null;
		String importFile = DEFAULT_IMPORT_FILE;

		String propertiesFile = null;
		String cfgXmlFile = null;
		String implicitNamingStrategyImplName = null;
		String physicalNamingStrategyImplName = null;

		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.equals( "--quiet" ) ) {
						parsedArgs.script = false;
					}
					else if ( arg.equals( "--drop" ) ) {
						parsedArgs.drop = true;
					}
					else if ( arg.equals( "--create" ) ) {
						parsedArgs.create = true;
					}
					else if ( arg.equals( "--schemas" ) ) {
						parsedArgs.exportSchemas = true;
					}
					else if ( arg.equals( "--haltonerror" ) ) {
						parsedArgs.halt = true;
					}
					else if ( arg.equals( "--text" ) ) {
						parsedArgs.export = false;
					}
					else if ( arg.startsWith( "--output=" ) ) {
						parsedArgs.outputFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--import=" ) ) {
						parsedArgs.importFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
					}
					else if ( arg.equals( "--format" ) ) {
						parsedArgs.format = true;
					}
					else if ( arg.startsWith( "--delimiter=" ) ) {
						parsedArgs.delimiter = arg.substring( 12 );
					}
					else if ( arg.startsWith( "--config=" ) ) {
						parsedArgs.cfgXmlFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--naming=" ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyArgument();
					}
					else if ( arg.startsWith( "--implicit-naming=" ) ) {
						parsedArgs.implicitNamingStrategyImplName = arg.substring( 18 );
					}
					else if ( arg.startsWith( "--physical-naming=" ) ) {
						parsedArgs.physicalNamingStrategyImplName = arg.substring( 18 );
					}
				}
				else {
					if ( arg.endsWith( ".jar" ) ) {
						parsedArgs.jarFiles.add( arg );
					}
					else {
						parsedArgs.hbmXmlFiles.add( arg );
					}
				}
			}

			return parsedArgs;
		}
	}
}
