/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.relational.spi.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.naming.Identifier;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromUrl;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputNonExistentImpl;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.tool.schema.internal.Helper.interpretScriptSourceSetting;

/**
 * This is functionally nothing more than the creation script from the older SchemaExport class (plus some
 * additional stuff in the script).
 *
 * @author Steve Ebersole
 */
public class SchemaCreatorImpl implements SchemaCreator {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SchemaCreatorImpl.class );

	public static final String DEFAULT_IMPORT_FILE = "/import.sql";

	private final HibernateSchemaManagementTool tool;
	private final DatabaseModel databaseModel;
	private final SchemaFilter schemaFilter;

	public SchemaCreatorImpl(HibernateSchemaManagementTool tool, DatabaseModel databaseModel) {
		this( tool, databaseModel, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaCreatorImpl(HibernateSchemaManagementTool tool, DatabaseModel databaseModel, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.databaseModel = databaseModel;
		this.schemaFilter = schemaFilter;
	}

	public SchemaCreatorImpl(DatabaseModel databaseModel, ServiceRegistry serviceRegistry) {
		this( databaseModel, serviceRegistry, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaCreatorImpl(DatabaseModel databaseModel, ServiceRegistry serviceRegistry, SchemaFilter schemaFilter) {
		SchemaManagementTool smt = serviceRegistry.getService( SchemaManagementTool.class );
		if ( smt == null || !HibernateSchemaManagementTool.class.isInstance( smt ) ) {
			smt = new HibernateSchemaManagementTool();
			( (HibernateSchemaManagementTool) smt ).injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}

		this.tool = (HibernateSchemaManagementTool) smt;
		this.databaseModel = databaseModel;
		this.schemaFilter = schemaFilter;
	}

	@Override
	public void doCreation(
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {
		if ( targetDescriptor.getTargetTypes().isEmpty() ) {
			return;
		}

		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
		final GenerationTarget[] targets = tool.buildGenerationTargets(
				targetDescriptor,
				jdbcContext,
				options.getConfigurationValues(),
				true
		);

		doCreation( jdbcContext.getDialect(), options, sourceDescriptor, targets );
	}

	public void doCreation(
			Dialect dialect,
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performCreation( dialect, options, sourceDescriptor, targets );
		}
		finally {
			for ( GenerationTarget target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
				}
			}
		}
	}

	private void performCreation(
			Dialect dialect,
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		final ImportSqlCommandExtractor commandExtractor = tool.getServiceRegistry().getService( ImportSqlCommandExtractor.class );

		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		switch ( sourceDescriptor.getSourceType() ) {
			case SCRIPT: {
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
				break;
			}
			case METADATA: {
				create( options, dialect, formatter, targets );
				break;
			}
			case METADATA_THEN_SCRIPT: {
				create( options, dialect, formatter, targets );
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
				break;
			}
			case SCRIPT_THEN_METADATA: {
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
				create( options, dialect, formatter, targets );
			}
		}

		applyImportSources( options, commandExtractor, format, targets );
	}

	public void createFromScript(
			ScriptSourceInput scriptSourceInput,
			ImportSqlCommandExtractor commandExtractor,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		scriptSourceInput.prepare();
		try {
			for ( String command : scriptSourceInput.read( commandExtractor ) ) {
				applySqlString( command, formatter, options, targets );
			}
		}
		finally {
			scriptSourceInput.release();
		}
	}

	public void create(
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		boolean tryToCreateCatalogs = false;
		boolean tryToCreateSchemas = false;
		if ( options.shouldManageNamespaces() ) {
			if ( dialect.canCreateSchema() ) {
				tryToCreateSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToCreateCatalogs = true;
			}
		}
		JdbcServices jdbcServices = tool.getServiceRegistry().getService( JdbcServices.class );

		final Set<String> exportIdentifiers = new HashSet<>( 50 );

		// first, create each catalog/schema
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<>();
			for ( Namespace namespace : databaseModel.getNamespaces() ) {

				if ( !schemaFilter.includeNamespace( namespace ) ) {
					continue;
				}

				if ( tryToCreateCatalogs ) {
					final Identifier catalogPhysicalName = namespace.getCatalogName();

					if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogPhysicalName ) ) {
						applySqlStrings(
								dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) ),
								formatter,
								options,
								targets
						);
						exportedCatalogs.add( catalogPhysicalName );
					}
				}

				if ( tryToCreateSchemas && namespace.getSchemaName() != null ) {
					applySqlStrings(
							dialect.getCreateSchemaCommand( namespace.getSchemaName().render( dialect ) ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		// next, create all "before table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				continue;
			}

			checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
			applySqlStrings(
					dialect.getAuxiliaryDatabaseObjectExporter().getSqlCreateStrings(
							auxiliaryDatabaseObject,
							jdbcServices
					),
					formatter,
					options,
					targets
			);
		}

		// then, create all schema objects (tables, sequences, constraints, etc) in each schema
		for ( Namespace namespace : databaseModel.getNamespaces() ) {

			if ( !schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			// sequences
			for ( Sequence sequence : namespace.getSequences() ) {
				if ( !schemaFilter.includeSequence( sequence ) ) {
					continue;
				}
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings(
						dialect.getSequenceExporter().getSqlCreateStrings( sequence, jdbcServices ),
//						dialect.getCreateSequenceStrings(
//								jdbcEnvironment.getQualifiedObjectNameFormatter().format( sequence.getName(), dialect ),
//								sequence.getInitialValue(),
//								sequence.getIncrementSize()
//						),
						formatter,
						options,
						targets
				);
			}

			final List<ExportableTable> exportableTables = namespace.getTables()
					.stream()
					.filter( table -> table.isExportable() && schemaFilter.includeTable( (ExportableTable) table ) )
					.map( table -> (ExportableTable)table )
					.collect(
							Collectors.toList() );
			// tables
			for ( ExportableTable exportableTable : exportableTables ) {
				checkExportIdentifier( exportableTable, exportIdentifiers );
				applySqlStrings(
						dialect.getTableExporter().getSqlCreateStrings( exportableTable, jdbcServices ),
						formatter,
						options,
						targets
				);

			}

			for ( ExportableTable exportableTable : exportableTables ) {
				// indexes
				for ( Index index : exportableTable.getIndexes() ) {
					checkExportIdentifier( index, exportIdentifiers );
					applySqlStrings(
							dialect.getIndexExporter().getSqlCreateStrings( index, jdbcServices ),
							formatter,
							options,
							targets
					);
				}

				// unique keys
				for ( UniqueKey ignored : exportableTable.getUniqueKeys() ) {
					checkExportIdentifier( ignored, exportIdentifiers );
					applySqlStrings(
							dialect.getUniqueKeyExporter().getSqlCreateStrings( ignored, jdbcServices ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		//NOTE : Foreign keys must be created *after* all tables of all namespaces for cross namespace fks. see HHH-10420
		for ( Namespace namespace : databaseModel.getNamespaces() ) {
			// NOTE : Foreign keys must be created *after* unique keys for numerous DBs.  See HHH-8390

			if ( !schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			for ( Table table : namespace.getTables() ) {
				if ( !table.isExportable() ) {
					continue;
				}
				if ( !schemaFilter.includeTable( (ExportableTable) table ) ) {
					continue;
				}
				// foreign keys
				final ExportableTable exportableTable = (ExportableTable) table;
				for ( ForeignKey foreignKey : exportableTable.getForeignKeys() ) {
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, jdbcServices ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		// next, create all "after table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlCreateStrings( auxiliaryDatabaseObject, jdbcServices ),
						formatter,
						options,
						targets
				);
			}
		}

		// and finally add all init commands
		for ( InitCommand initCommand : databaseModel.getInitCommands() ) {
			// todo: this should alo probably use the DML formatter...
			applySqlStrings( initCommand.getInitCommands(), formatter, options, targets );
		}
	}

	private static void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException( "SQL strings added more than once for: " + exportIdentifier );
		}
		exportIdentifiers.add( exportIdentifier );
	}

	private static void applySqlStrings(
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings == null ) {
			return;
		}

		for ( String sqlString : sqlStrings ) {
			applySqlString( sqlString, formatter, options, targets );
		}
	}

	private static void applySqlString(
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( StringHelper.isEmpty( sqlString ) ) {
			return;
		}

		try {
			String sqlStringFormatted = formatter.format( sqlString );
			for ( GenerationTarget target : targets ) {
				target.accept( sqlStringFormatted );
			}
		}
		catch (CommandAcceptanceException e) {
			options.getExceptionHandler().handleException( e );
		}
	}

	private void applyImportSources(
			ExecutionOptions options,
			ImportSqlCommandExtractor commandExtractor,
			boolean format,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		// I have had problems applying the formatter to these imported statements.
		// and legacy SchemaExport did not format them, so doing same here
		//final Formatter formatter = format ? DDLFormatterImpl.INSTANCE : FormatStyle.NONE.getFormatter();
		final Formatter formatter = FormatStyle.NONE.getFormatter();

		final Object importScriptSetting = options.getConfigurationValues().get( HBM2DDL_LOAD_SCRIPT_SOURCE );
		String charsetName = (String) options.getConfigurationValues().get( HBM2DDL_CHARSET_NAME );

		if ( importScriptSetting != null ) {
			final ScriptSourceInput importScriptInput = interpretScriptSourceSetting( importScriptSetting, classLoaderService, charsetName );
			log.executingImportScript( importScriptInput.toString() );
			importScriptInput.prepare();
			try {
				for ( String command : importScriptInput.read( commandExtractor ) ) {
					applySqlString( command, formatter, options, targets );
				}
			}
			finally {
				importScriptInput.release();
			}
		}

		final String importFiles = ConfigurationHelper.getString(
				AvailableSettings.HBM2DDL_IMPORT_FILES,
				options.getConfigurationValues(),
				DEFAULT_IMPORT_FILE
		);

		for ( String currentFile : importFiles.split( "," ) ) {
			final String resourceName = currentFile.trim();
			if ( "".equals( resourceName ) ) {
				//skip empty resource names
				continue;
			}
			final ScriptSourceInput importScriptInput = interpretLegacyImportScriptSetting( resourceName, classLoaderService, charsetName );
			importScriptInput.prepare();
			try {
				log.executingImportScript( importScriptInput.toString() );
				for ( String command : importScriptInput.read( commandExtractor ) ) {
					applySqlString( command, formatter, options, targets );
				}
			}
			finally {
				importScriptInput.release();
			}
		}
	}

	private ScriptSourceInput interpretLegacyImportScriptSetting(
			String resourceName,
			ClassLoaderService classLoaderService,
			String charsetName) {
		try {
			final URL resourceUrl = classLoaderService.locateResource( resourceName );
			if ( resourceUrl == null ) {
				return ScriptSourceInputNonExistentImpl.INSTANCE;
			}
			else {
				return new ScriptSourceInputFromUrl( resourceUrl, charsetName );
			}
		}
		catch (Exception e) {
			throw new SchemaManagementException( "Error resolving legacy import resource : " + resourceName, e );
		}
	}

	/**
	 * For testing...
	 *
	 * @param metadata The metadata for which to generate the creation commands.
	 *
	 * @return The generation commands
	 */
	public List<String> generateCreationCommands(Metadata metadata, final boolean manageNamespaces) {
		final JournalingGenerationTarget target = new JournalingGenerationTarget();

		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
				.getServiceRegistry();
		final Dialect dialect = serviceRegistry.getService( JdbcEnvironment.class ).getDialect();

		final ExecutionOptions options = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return manageNamespaces;
			}

			@Override
			public Map getConfigurationValues() {
				return Collections.emptyMap();
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerHaltImpl.INSTANCE;
			}
		};

		create( options, dialect, FormatStyle.NONE.getFormatter(), target );

		return target.commands;
	}


	public void doCreation(
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
		doCreation(
				serviceRegistry,
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	public void doCreation(
			final ServiceRegistry serviceRegistry,
			final Map settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		doCreation(
				serviceRegistry.getService( JdbcEnvironment.class ).getDialect(),
				new ExecutionOptions() {
					@Override
					public boolean shouldManageNamespaces() {
						return manageNamespaces;
					}

					@Override
					public Map getConfigurationValues() {
						return settings;
					}

					@Override
					public ExceptionHandler getExceptionHandler() {
						return ExceptionHandlerLoggedImpl.INSTANCE;
					}
				},
				new SourceDescriptor() {
					@Override
					public SourceType getSourceType() {
						return SourceType.METADATA;
					}

					@Override
					public ScriptSourceInput getScriptSourceInput() {
						return null;
					}
				},
				targets
		);
	}

	private static class JournalingGenerationTarget implements GenerationTarget {
		private final ArrayList<String> commands = new ArrayList<>();

		@Override
		public void prepare() {
		}

		@Override
		public void accept(String command) {
			commands.add( command );
		}

		@Override
		public void release() {
		}
	}

}
