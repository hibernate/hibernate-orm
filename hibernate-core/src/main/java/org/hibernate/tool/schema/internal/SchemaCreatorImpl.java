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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
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
	private final SchemaFilter schemaFilter;

	public SchemaCreatorImpl(HibernateSchemaManagementTool tool) {
		this( tool, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaCreatorImpl(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter;
	}

	public SchemaCreatorImpl(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaCreatorImpl(ServiceRegistry serviceRegistry, SchemaFilter schemaFilter) {
		SchemaManagementTool smt = serviceRegistry.getService( SchemaManagementTool.class );
		if ( smt == null || !HibernateSchemaManagementTool.class.isInstance( smt ) ) {
			smt = new HibernateSchemaManagementTool();
			( (HibernateSchemaManagementTool) smt ).injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}

		this.tool = (HibernateSchemaManagementTool) smt;
		this.schemaFilter = schemaFilter;
	}

	@Override
	public void doCreation(
			Metadata metadata,
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

		doCreation( metadata, jdbcContext.getDialect(), options, sourceDescriptor, targets );
	}

	public void doCreation(
			Metadata metadata,
			Dialect dialect,
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performCreation( metadata, dialect, options, sourceDescriptor, targets );
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
			Metadata metadata,
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
				createFromMetadata( metadata, options, dialect, formatter, targets );
				break;
			}
			case METADATA_THEN_SCRIPT: {
				createFromMetadata( metadata, options, dialect, formatter, targets );
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
				break;
			}
			case SCRIPT_THEN_METADATA: {
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
				createFromMetadata( metadata, options, dialect, formatter, targets );
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

	public void createFromMetadata(
			Metadata metadata,
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

		final Database database = metadata.getDatabase();

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		// first, create each catalog/schema
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<Identifier>();
			for ( Namespace namespace : database.getNamespaces() ) {

				if ( !schemaFilter.includeNamespace( namespace ) ) {
					continue;
				}

				if ( tryToCreateCatalogs ) {
					final Identifier catalogLogicalName = namespace.getName().getCatalog();
					final Identifier catalogPhysicalName = namespace.getPhysicalName().getCatalog();

					if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName ) ) {
						applySqlStrings(
								dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) ),
								formatter,
								options,
								targets
						);
						exportedCatalogs.add( catalogLogicalName );
					}
				}

				if ( tryToCreateSchemas && namespace.getPhysicalName().getSchema() != null ) {
					applySqlStrings(
							dialect.getCreateSchemaCommand( namespace.getPhysicalName().getSchema().render( dialect ) ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		// next, create all "before table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}

			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlCreateStrings(
								auxiliaryDatabaseObject,
								metadata
						),
						formatter,
						options,
						targets
				);
			}
		}

		// then, create all schema objects (tables, sequences, constraints, etc) in each schema
		for ( Namespace namespace : database.getNamespaces() ) {

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
						dialect.getSequenceExporter().getSqlCreateStrings(
								sequence,
								metadata
						),
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

			// tables
			for ( Table table : namespace.getTables() ) {
				if ( !table.isPhysicalTable() ){
					continue;
				}
				if ( !schemaFilter.includeTable( table ) ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings(
						dialect.getTableExporter().getSqlCreateStrings( table, metadata ),
						formatter,
						options,
						targets
				);

			}

			for ( Table table : namespace.getTables() ) {
				if ( !table.isPhysicalTable() ){
					continue;
				}
				if ( !schemaFilter.includeTable( table ) ) {
					continue;
				}
				// indexes
				final Iterator indexItr = table.getIndexIterator();
				while ( indexItr.hasNext() ) {
					final Index index = (Index) indexItr.next();
					checkExportIdentifier( index, exportIdentifiers );
					applySqlStrings(
							dialect.getIndexExporter().getSqlCreateStrings( index, metadata ),
							formatter,
							options,
							targets
					);
				}

				// unique keys
				final Iterator ukItr = table.getUniqueKeyIterator();
				while ( ukItr.hasNext() ) {
					final UniqueKey uniqueKey = (UniqueKey) ukItr.next();
					checkExportIdentifier( uniqueKey, exportIdentifiers );
					applySqlStrings(
							dialect.getUniqueKeyExporter().getSqlCreateStrings( uniqueKey, metadata ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		//NOTE : Foreign keys must be created *after* all tables of all namespaces for cross namespace fks. see HHH-10420
		for ( Namespace namespace : database.getNamespaces() ) {
			// NOTE : Foreign keys must be created *after* unique keys for numerous DBs.  See HHH-8390

			if ( !schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			for ( Table table : namespace.getTables() ) {
				if ( !schemaFilter.includeTable( table ) ) {
					continue;
				}
				// foreign keys
				final Iterator fkItr = table.getForeignKeyIterator();
				while ( fkItr.hasNext() ) {
					final ForeignKey foreignKey = (ForeignKey) fkItr.next();
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, metadata ),
							formatter,
							options,
							targets
					);
				}
			}
		}

		// next, create all "after table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect )
					&& !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlCreateStrings( auxiliaryDatabaseObject, metadata ),
						formatter,
						options,
						targets
				);
			}
		}

		// and finally add all init commands
		for ( InitCommand initCommand : database.getInitCommands() ) {
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

		createFromMetadata( metadata, options, dialect, FormatStyle.NONE.getFormatter(), target );

		return target.commands;
	}


	public void doCreation(
			Metadata metadata,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();
		doCreation(
				metadata,
				serviceRegistry,
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	public void doCreation(
			Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		doCreation(
				metadata,
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
		private final ArrayList<String> commands = new ArrayList<String>();

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
