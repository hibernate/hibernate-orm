/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.tool.schema.internal.Helper.applyScript;
import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;
import static org.hibernate.tool.schema.internal.Helper.interpretFormattingEnabled;

/**
 * Basic implementation of {@link SchemaCreator}.
 *
 * @author Steve Ebersole
 */
public class SchemaCreatorImpl extends AbstractSchemaPopulator implements SchemaCreator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaCreatorImpl.class );

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
		if ( serviceRegistry.getService( SchemaManagementTool.class )
				instanceof HibernateSchemaManagementTool schemaManagementTool ) {
			tool = schemaManagementTool;
		}
		else {
			tool = new HibernateSchemaManagementTool();
			tool.injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}
		this.schemaFilter = schemaFilter;
	}

	@Override
	public void doCreation(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final Map<String, Object> configuration = options.getConfigurationValues();
			final JdbcContext jdbcContext = tool.resolveJdbcContext( configuration );
			doCreation(
					metadata,
					jdbcContext.getDialect(),
					options,
					contributableInclusionFilter,
					sourceDescriptor,
					tool.buildGenerationTargets( targetDescriptor, jdbcContext, configuration, true )
			);
		}
	}

	@Internal
	public void doCreation(
			Metadata metadata,
			Dialect dialect,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performCreation( metadata, dialect, options, contributableInclusionFilter, sourceDescriptor, targets );
		}
		finally {
			for ( GenerationTarget target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					LOG.debugf( "Problem releasing GenerationTarget [%s]: %s", target, e.getMessage() );
				}
			}
		}
	}

	private void performCreation(
			Metadata metadata,
			Dialect dialect,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		final SqlScriptCommandExtractor commandExtractor = getCommandExtractor();
		final boolean format = interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		switch ( sourceDescriptor.getSourceType() ) {
			case SCRIPT:
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
				break;
			case METADATA:
				createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
				break;
			case METADATA_THEN_SCRIPT:
				createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
				break;
			case SCRIPT_THEN_METADATA:
				createFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, dialect, options, targets );
				createFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
				break;
		}

		applyImportSources( options, commandExtractor, format, dialect, targets );
	}

	private SqlScriptCommandExtractor getCommandExtractor() {
		return tool.getServiceRegistry().getService( SqlScriptCommandExtractor.class );
	}

	@Override
	ClassLoaderService getClassLoaderService() {
		return tool.getServiceRegistry().getService( ClassLoaderService.class );
	}

	public void createFromScript(
			ScriptSourceInput scriptSourceInput,
			SqlScriptCommandExtractor commandExtractor,
			Formatter formatter,
			Dialect dialect,
			ExecutionOptions options,
			GenerationTarget... targets) {
		applyScript( options, commandExtractor, dialect, scriptSourceInput, formatter, targets );
	}

	@Internal
	public void createFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		createFromMetadata(
				metadata,
				options,
				(contributed) -> true,
				dialect,
				formatter,
				targets
		);
	}

	@Internal
	public void createFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		final SqlStringGenerationContext context = createSqlStringGenerationContext(options, metadata);
		final Set<String> exportIdentifiers = setOfSize(50);

		createSchemasAndCatalogs(metadata, options, schemaFilter, dialect, formatter, context, targets);
		// next, create all "before table" auxiliary objects
		createAuxiliaryObjectsBeforeTables(metadata, options, dialect, formatter, context, exportIdentifiers, targets);
		// next, create all UDTs
		createUserDefinedTypes(metadata, options, schemaFilter, dialect, formatter, context, targets);
		// then, create all schema objects (tables, sequences, constraints, etc) in each schema
		createSequencesTablesConstraints(
				metadata,
				options,
				schemaFilter,
				contributableInclusionMatcher,
				dialect,
				formatter,
				context,
				exportIdentifiers,
				targets
		);
		// foreign keys must be created after all tables of all namespaces for cross-namespace constraints (see HHH-10420)
		createForeignKeys( metadata, options, schemaFilter, contributableInclusionMatcher, dialect, formatter, context, targets );
		// next, create all "after table" auxiliary objects
		createAuxiliaryObjectsAfterTables( metadata, options, dialect, formatter, context, exportIdentifiers, targets );
		// and finally add all init commands
		executeInitCommands(metadata, options, formatter, targets);
	}

	private static void executeInitCommands(Metadata metadata, ExecutionOptions options, Formatter formatter, GenerationTarget[] targets) {
		for ( InitCommand initCommand : metadata.getDatabase().getInitCommands() ) {
			// todo: this should alo probably use the DML formatter...
			applySqlStrings( initCommand.initCommands(), formatter, options, targets);
		}
	}

	private static void createAuxiliaryObjectsAfterTables(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets) {
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : metadata.getDatabase().getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect )
					&& !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlCreateStrings( auxiliaryDatabaseObject, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void createForeignKeys(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			// foreign keys must be created after unique keys for numerous DBs (see HHH-8390)
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Table table : namespace.getTables() ) {
					if ( schemaFilter.includeTable( table )
							&& contributableInclusionMatcher.matches( table ) ) {
						// foreign keys
						for ( ForeignKey foreignKey : table.getForeignKeyCollection() ) {
							applySqlStrings(
									dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, metadata, context ),
									formatter,
									options,
									targets
							);
						}
					}
				}
			}
		}
	}

	private static void createSequencesTablesConstraints(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				// sequences
				createSequences(
						metadata,
						options,
						schemaFilter,
						contributableInclusionMatcher,
						dialect,
						formatter,
						context,
						exportIdentifiers,
						targets,
						namespace
				);
				// tables
				createTables(
						metadata,
						options,
						schemaFilter,
						contributableInclusionMatcher,
						dialect,
						formatter,
						context,
						exportIdentifiers,
						targets,
						namespace
				);
				createTableConstraints(
						metadata,
						options,
						schemaFilter,
						contributableInclusionMatcher,
						dialect,
						formatter,
						context,
						exportIdentifiers,
						targets,
						namespace
				);
			}
		}
	}

	private static void createTableConstraints(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets,
			Namespace namespace) {
		for ( Table table : namespace.getTables() ) {
			if ( table.isPhysicalTable()
					&& schemaFilter.includeTable( table )
					&& contributableInclusionMatcher.matches( table ) ) {
				// indexes
				for ( Index index : table.getIndexes().values() ) {
					checkExportIdentifier( index, exportIdentifiers );
					applySqlStrings(
							dialect.getIndexExporter().getSqlCreateStrings( index, metadata, context ),
							formatter,
							options,
							targets
					);
				}
				// unique keys
				for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
					checkExportIdentifier( uniqueKey, exportIdentifiers );
					applySqlStrings(
							dialect.getUniqueKeyExporter().getSqlCreateStrings( uniqueKey, metadata, context ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private static void createTables(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets,
			Namespace namespace) {
		for ( Table table : namespace.getTables() ) {
			if ( table.isPhysicalTable()
					&& !table.isView()
					&& schemaFilter.includeTable( table )
					&& contributableInclusionMatcher.matches( table ) ) {
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings(
						dialect.getTableExporter().getSqlCreateStrings( table, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
		for ( Table table : namespace.getTables() ) {
			if ( table.isPhysicalTable()
					&& table.isView()
					&& schemaFilter.includeTable( table )
					&& contributableInclusionMatcher.matches( table ) ) {
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings(
						dialect.getTableExporter().getSqlCreateStrings( table, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void createSequences(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionMatcher,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets,
			Namespace namespace) {
		for ( Sequence sequence : namespace.getSequences() ) {
			if ( schemaFilter.includeSequence( sequence )
					&& contributableInclusionMatcher.matches( sequence ) ) {
				checkExportIdentifier( sequence, exportIdentifiers);
				applySqlStrings(
						dialect.getSequenceExporter().getSqlCreateStrings( sequence, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void createAuxiliaryObjectsBeforeTables(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			Set<String> exportIdentifiers,
			GenerationTarget[] targets) {
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : metadata.getDatabase().getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation()
					&& auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlCreateStrings( auxiliaryDatabaseObject, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	static void createUserDefinedTypes(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( UserDefinedType userDefinedType : namespace.getDependencyOrderedUserDefinedTypes() ) {
					applySqlStrings(
							dialect.getUserDefinedTypeExporter()
									.getSqlCreateStrings( userDefinedType, metadata, context ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private static void createSchemasAndCatalogs(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		final boolean tryToCreateCatalogs = options.shouldManageNamespaces() && dialect.canCreateCatalog();
		final boolean tryToCreateSchemas = options.shouldManageNamespaces() && dialect.canCreateSchema();
		// first, create each catalog/schema
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<>();
			for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
				if ( schemaFilter.includeNamespace( namespace ) ) {
					Namespace.Name logicalName = namespace.getName();
					Namespace.Name physicalName = namespace.getPhysicalName();

					if ( tryToCreateCatalogs ) {
						final Identifier catalogLogicalName = logicalName.catalog();
						final Identifier catalogPhysicalName = context.catalogWithDefault( physicalName.catalog() );
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

					if ( tryToCreateSchemas ) {
						final Identifier schemaPhysicalName = context.schemaWithDefault( physicalName.schema() );
						if ( schemaPhysicalName != null ) {
							applySqlStrings(
									dialect.getCreateSchemaCommand( schemaPhysicalName.render( dialect ) ),
									formatter,
									options,
									targets
							);
						}
					}
				}
			}
		}
	}

	private static void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException( "SQL strings added more than once for: " + exportIdentifier );
		}
		exportIdentifiers.add( exportIdentifier );
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

		final MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		final Dialect dialect =
				metadataImplementor.getMetadataBuildingOptions()
						.getServiceRegistry()
						.requireService( JdbcEnvironment.class )
						.getDialect();

		final ExecutionOptions options = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return manageNamespaces;
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
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

	/**
	 * Intended for use from tests
	 */
	@Internal
	public void doCreation(
			Metadata metadata,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry =
				( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
						.getServiceRegistry();
		doCreation(
				metadata,
				serviceRegistry,
				serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	/**
	 * Intended for use from tests
	 */
	@Internal
	public void doCreation(
			Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map<String,Object> settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		doCreation(
				metadata,
				serviceRegistry.requireService( JdbcEnvironment.class ).getDialect(),
				new ExecutionOptions() {
					@Override
					public boolean shouldManageNamespaces() {
						return manageNamespaces;
					}

					@Override
					public Map<String,Object> getConfigurationValues() {
						return settings;
					}

					@Override
					public ExceptionHandler getExceptionHandler() {
						return ExceptionHandlerLoggedImpl.INSTANCE;
					}
				},
				(contributed) -> true,
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
