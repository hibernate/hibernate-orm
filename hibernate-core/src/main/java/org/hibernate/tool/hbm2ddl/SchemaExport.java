/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * Command-line tool for exporting (create and/or drop) a database schema.  The export can
 * be sent directly to the database, written to script or both.
 *
 * @author Daniel Bradby
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SchemaExport {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaExport.class );

	private final DatabaseModel databaseModel;
	private final ServiceRegistry serviceRegistry;

	public SchemaExport(DatabaseModel databaseModel, ServiceRegistry serviceRegistry) {
		this.databaseModel = databaseModel;
		this.serviceRegistry = serviceRegistry;
	}

	public enum Type {
		CREATE( Action.CREATE ),
		DROP( Action.DROP ),
		NONE( Action.NONE ),
		BOTH( Action.BOTH );

		private final Action actionReplacement;

		Type(Action actionReplacement) {
			this.actionReplacement = actionReplacement;
		}

		public boolean doCreate() {
			return actionReplacement.doCreate();
		}

		public boolean doDrop() {
			return actionReplacement.doDrop();
		}
	}

	public enum Action {
		/**
		 * None - duh :P
		 */
		NONE,
		/**
		 * Create only
		 */
		CREATE,
		/**
		 * Drop only
		 */
		DROP,
		/**
		 * Drop and then create
		 */
		BOTH;

		public boolean doCreate() {
			return this == BOTH || this == CREATE;
		}

		public boolean doDrop() {
			return this == BOTH || this == DROP;
		}

		private static Action interpret(boolean justDrop, boolean justCreate) {
			if ( justDrop ) {
				return Action.DROP;
			}
			else if ( justCreate ) {
				return Action.CREATE;
			}
			else {
				return Action.BOTH;
			}
		}

		public static Action parseCommandLineOption(String actionText) {
			if ( actionText.equalsIgnoreCase( "create" ) ) {
				return CREATE;
			}
			else if ( actionText.equalsIgnoreCase( "drop" ) ) {
				return DROP;
			}
			else if ( actionText.equalsIgnoreCase( "drop-and-create" ) ) {
				return BOTH;
			}
			else {
				return NONE;
			}
		}
	}

	boolean haltOnError = false;
	boolean format = false;
	boolean manageNamespaces = false;
	String delimiter = null;

	String outputFile = null;

	private String importFiles;

	private final List<Exception> exceptions = new ArrayList<Exception>();


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
	 * Comma-separated list of resource names to use for database init commands on create.
	 *
	 * @param importFiles The comma-separated list of init file resources names
	 *
	 * @return this
	 */
	public SchemaExport setImportFiles(String importFiles) {
		this.importFiles = importFiles;
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
		this.format = format;
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

	public SchemaExport setManageNamespaces(boolean manageNamespaces) {
		this.manageNamespaces = manageNamespaces;
		return this;
	}

	public void drop(EnumSet<TargetType> targetTypes) {
		execute( targetTypes, Action.DROP );
	}

	public void create(EnumSet<TargetType> targetTypes) {
		execute( targetTypes, Action.BOTH );
	}

	public void createOnly(EnumSet<TargetType> targetTypes) {
		execute( targetTypes, Action.CREATE );
	}

	@SuppressWarnings("unchecked")
	public void execute(EnumSet<TargetType> targetTypes, Action action) {
		if ( action == Action.NONE ) {
			LOG.debug( "Skipping SchemaExport as Action.NONE was passed" );
			return;
		}

		if ( targetTypes.isEmpty() ) {
			LOG.debug( "Skipping SchemaExport as no targets were specified" );
			return;
		}

		exceptions.clear();

		LOG.runningHbm2ddlSchemaExport();

		final TargetDescriptor targetDescriptor = buildTargetDescriptor( targetTypes, outputFile, serviceRegistry );

		doExecution( action, needsJdbcConnection( targetTypes ), targetDescriptor );
	}

	public void doExecution(
			Action action,
			boolean needsJdbc,
			TargetDescriptor targetDescriptor) {
		Map config = new HashMap();
		config.putAll( serviceRegistry.getService( ConfigurationService.class ).getSettings() );

		config.put( AvailableSettings.HBM2DDL_DELIMITER, delimiter );
		config.put( AvailableSettings.FORMAT_SQL, format );
		config.put( AvailableSettings.HBM2DDL_IMPORT_FILES, importFiles );

		final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );

		final ExceptionHandler exceptionHandler = haltOnError
				? ExceptionHandlerHaltImpl.INSTANCE
				: new ExceptionHandlerCollectingImpl();
		final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
				config,
				exceptionHandler
		);

		final SourceDescriptor sourceDescriptor = new SourceDescriptor() {
			@Override
			public SourceType getSourceType() {
				return SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};

		try {
			if ( action.doDrop() ) {
				tool.getSchemaDropper( databaseModel, config ).doDrop(
						executionOptions,
						sourceDescriptor,
						targetDescriptor
				);
			}

			if ( action.doCreate() ) {
				tool.getSchemaCreator( databaseModel, config ).doCreation(
						executionOptions,
						sourceDescriptor,
						targetDescriptor
				);
			}
		}
		finally {
			if ( exceptionHandler instanceof ExceptionHandlerCollectingImpl ) {
				exceptions.addAll( ( (ExceptionHandlerCollectingImpl) exceptionHandler ).getExceptions() );
			}
		}
	}

	private boolean needsJdbcConnection(EnumSet<TargetType> targetTypes) {
		return targetTypes.contains( TargetType.DATABASE );
	}

	public static TargetDescriptor buildTargetDescriptor(
			EnumSet<TargetType> targetTypes,
			String outputFile,
			ServiceRegistry serviceRegistry) {
		final ScriptTargetOutput scriptTarget;
		if ( targetTypes.contains( TargetType.SCRIPT ) ) {
			if ( outputFile == null ) {
				throw new SchemaManagementException(
						"Writing to script was requested, but no script file was specified, test class should override createSqlScriptTempOutputFile method" );
			}
			scriptTarget = Helper.interpretScriptTargetSetting(
					outputFile,
					serviceRegistry.getService( ClassLoaderService.class ),
					(String) serviceRegistry.getService( ConfigurationService.class )
							.getSettings()
							.get( AvailableSettings.HBM2DDL_CHARSET_NAME )
			);
		}
		else {
			scriptTarget = null;
		}

		return new TargetDescriptorImpl( targetTypes, scriptTarget );
	}

	/**
	 * For testing use
	 */
	public void perform(Action action, ScriptTargetOutput target) {
		doExecution(
				action,
				false,
				new TargetDescriptorImpl( EnumSet.of( TargetType.SCRIPT ), target )
		);
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
			execute( commandLineArgs );
		}
		catch (Exception e) {
			LOG.unableToCreateSchema( e );
		}
	}

	public static void execute(CommandLineArgs commandLineArgs) throws Exception {
		StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
		try {
			final MetadataImplementor metadata = buildMetadata( commandLineArgs, serviceRegistry );

			new SchemaExport( Helper.buildDatabaseModel( metadata ), serviceRegistry )
					.setHaltOnError( commandLineArgs.halt )
					.setOutputFile( commandLineArgs.outputFile )
					.setDelimiter( commandLineArgs.delimiter )
					.setFormat( commandLineArgs.format )
					.setManageNamespaces( commandLineArgs.manageNamespaces )
					.setImportFiles( commandLineArgs.importFile )
					.execute( commandLineArgs.targetTypes, commandLineArgs.action );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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
			final MetadataImplementor metadataImplementor = buildMetadata( commandLineArgs, serviceRegistry );
			return metadataImplementor;
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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

	private static class CommandLineArgs {
		EnumSet<TargetType> targetTypes;
		Action action;

		boolean halt = false;
		boolean format = false;

		boolean manageNamespaces = false;

		String delimiter = null;

		String outputFile = null;
		String importFile = SchemaCreatorImpl.DEFAULT_IMPORT_FILE;

		String propertiesFile = null;
		String cfgXmlFile = null;
		String implicitNamingStrategyImplName = null;
		String physicalNamingStrategyImplName = null;

		List<String> hbmXmlFiles = new ArrayList<>();
		List<String> jarFiles = new ArrayList<>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			String targetText = null;
			boolean script = true;
			boolean export = true;

			String actionText = null;
			boolean drop = false;
			boolean create = false;

			CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.equals( "--quiet" ) ) {
						script = false;
					}
					else if ( arg.equals( "--text" ) ) {
						export = false;
					}
					else if ( arg.equals( "--drop" ) ) {
						drop = true;
					}
					else if ( arg.equals( "--create" ) ) {
						create = true;
					}
					else if ( arg.startsWith( "--action=" ) ) {
						actionText = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--target=" ) ) {
						targetText = arg.substring( 9 );
					}
					else if ( arg.equals( "--schemas" ) ) {
						parsedArgs.manageNamespaces = true;
					}
					else if ( arg.equals( "--haltonerror" ) ) {
						parsedArgs.halt = true;
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

			if ( actionText == null ) {
				parsedArgs.action = Action.interpret( drop, create );
			}
			else {
				if ( drop || create ) {
					LOG.warn( "--drop or --create was used; prefer --action=none|create|drop|drop-and-create instead" );
				}
				parsedArgs.action = Action.parseCommandLineOption( actionText );
			}

			if ( targetText == null ) {
				parsedArgs.targetTypes = TargetTypeHelper.parseLegacyCommandLineOptions(
						script,
						export,
						parsedArgs.outputFile
				);
			}
			else {
				if ( !script || !export ) {
					LOG.warn( "--text or --quiet was used; prefer --target=none|(stdout|database|script)*" );
				}
				parsedArgs.targetTypes = TargetTypeHelper.parseCommandLineOptions( targetText );
			}

			return parsedArgs;
		}
	}

	private static class TargetDescriptorImpl implements TargetDescriptor {
		private final EnumSet<TargetType> targetTypes;
		private final ScriptTargetOutput scriptTarget;

		public TargetDescriptorImpl(
				EnumSet<TargetType> targetTypes,
				ScriptTargetOutput scriptTarget) {

			this.targetTypes = targetTypes;
			this.scriptTarget = scriptTarget;
		}

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return targetTypes;
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return scriptTarget;
		}
	}
}
