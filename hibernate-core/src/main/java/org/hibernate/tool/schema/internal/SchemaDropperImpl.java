/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.tool.schema.internal.Helper.applyScript;
import static org.hibernate.tool.schema.internal.Helper.applySqlString;
import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;
import static org.hibernate.tool.schema.internal.Helper.interpretFormattingEnabled;

/**
 * Basic implementation of {@link SchemaDropper}.
 *
 * @author Steve Ebersole
 */
public class SchemaDropperImpl implements SchemaDropper {
	private static final Logger log = Logger.getLogger( SchemaDropperImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	public SchemaDropperImpl(HibernateSchemaManagementTool tool) {
		this( tool, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaDropperImpl(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter;
	}

	public SchemaDropperImpl(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaDropperImpl(ServiceRegistry serviceRegistry, SchemaFilter schemaFilter) {
		SchemaManagementTool smt = serviceRegistry.getService( SchemaManagementTool.class );
		if ( !(smt instanceof HibernateSchemaManagementTool) ) {
			smt = new HibernateSchemaManagementTool();
			( (HibernateSchemaManagementTool) smt ).injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}

		this.tool = (HibernateSchemaManagementTool) smt;
		this.schemaFilter = schemaFilter;
	}

	@Override
	public void doDrop(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final Map<String, Object> configuration = options.getConfigurationValues();
			final JdbcContext jdbcContext = tool.resolveJdbcContext( configuration );
			doDrop(
					metadata,
					options,
					inclusionFilter,
					jdbcContext.getDialect(),
					sourceDescriptor,
					tool.buildGenerationTargets( targetDescriptor, jdbcContext, configuration, true )
			);
		}
	}

	/**
	 * For use from testing
	 */
	@Internal
	public void doDrop(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		doDrop( metadata, options, contributed -> true, dialect, sourceDescriptor, targets );
	}

	/**
	 * For use from testing
	 */
	@Internal
	public void doDrop(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performDrop( metadata, options, inclusionFilter, dialect, sourceDescriptor, targets );
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

	private void performDrop(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		final SqlScriptCommandExtractor commandExtractor = getCommandExtractor();
		final boolean format = interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		switch ( sourceDescriptor.getSourceType() ) {
			case SCRIPT:
				applyScript( options, commandExtractor, dialect, sourceDescriptor.getScriptSourceInput(), formatter, targets );
				break;
			case METADATA:
				dropFromMetadata( metadata, options, inclusionFilter, dialect, formatter, targets );
				break;
			case METADATA_THEN_SCRIPT:
				dropFromMetadata( metadata, options, inclusionFilter, dialect, formatter, targets );
				applyScript( options, commandExtractor, dialect, sourceDescriptor.getScriptSourceInput(), formatter, targets );
				break;
			case SCRIPT_THEN_METADATA:
				applyScript( options, commandExtractor, dialect, sourceDescriptor.getScriptSourceInput(), formatter, targets );
				dropFromMetadata( metadata, options, inclusionFilter, dialect, formatter, targets );
				break;
		}
	}

	private SqlScriptCommandExtractor getCommandExtractor() {
		return tool.getServiceRegistry().getService(SqlScriptCommandExtractor.class);
	}

	private void dropFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {

		// NOTE : init commands are irrelevant for dropping...

		applySqlString( dialect.getBeforeDropStatement(), formatter, options, targets );

		final SqlStringGenerationContext context = createSqlStringGenerationContext( options, metadata );
		// Reverse the list on drop to retain possible dependencies
		dropAuxiliaryObjectsBeforeTables( metadata, options, dialect, formatter, context, targets );
		dropConstraintsTablesSequences(
				metadata,
				options,
				inclusionFilter,
				dialect,
				formatter,
				context,
				targets
		);
		dropAuxiliaryObjectsAfterTables( metadata, options, dialect, formatter, context, targets );
		dropUserDefinedTypes( metadata, options, schemaFilter, dialect, formatter, context, targets );
		dropSchemasAndCatalogs( metadata, options, schemaFilter, dialect, formatter, context, targets );
	}

	private void dropConstraintsTablesSequences(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		final Set<String> exportIdentifiers = setOfSize( 50 );
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {

				// we need to drop all constraints/indexes prior to dropping the tables
				applyConstraintDropping(
						namespace,
						metadata,
						formatter,
						options,
						context,
						inclusionFilter,
						targets
				);

				// now it's safe to drop the tables
				dropTables(
						metadata,
						options,
						schemaFilter,
						inclusionFilter,
						dialect,
						formatter,
						exportIdentifiers,
						context,
						namespace,
						targets
				);

				dropSequences(
						metadata,
						options,
						schemaFilter,
						inclusionFilter,
						dialect,
						formatter,
						exportIdentifiers,
						context,
						namespace,
						targets
				);
			}
		}
	}

	private static void dropAuxiliaryObjectsBeforeTables(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject :
				reverse( metadata.getDatabase().getAuxiliaryDatabaseObjects() ) ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation()
					&& auxiliaryDatabaseObject.appliesToDialect(dialect) ) {
				applySqlStrings(
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlDropStrings( auxiliaryDatabaseObject, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void dropAuxiliaryObjectsAfterTables(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject :
				reverse( metadata.getDatabase().getAuxiliaryDatabaseObjects() ) ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation()
					&& auxiliaryDatabaseObject.appliesToDialect(dialect) ) {
				applySqlStrings(
						auxiliaryDatabaseObject.sqlDropStrings( context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void dropSequences(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			SqlStringGenerationContext context,
			Namespace namespace,
			GenerationTarget[] targets) {
		for ( Sequence sequence : namespace.getSequences() ) {
			if ( schemaFilter.includeSequence( sequence )
					&& inclusionFilter.matches( sequence ) ) {
				checkExportIdentifier( sequence, exportIdentifiers);
				applySqlStrings(
						dialect.getSequenceExporter().getSqlDropStrings( sequence, metadata, context ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void dropTables(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher inclusionFilter,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			SqlStringGenerationContext context,
			Namespace namespace,
			GenerationTarget[] targets) {
		for ( Table table : namespace.getTables() ) {
			if ( table.isPhysicalTable()
					&& table.isView()
					&& schemaFilter.includeTable( table )
					&& inclusionFilter.matches( table ) ) {
				checkExportIdentifier( table, exportIdentifiers);
				applySqlStrings(
						dialect.getTableExporter().getSqlDropStrings( table, metadata, context),
						formatter,
						options,
						targets
				);
			}
		}
		for ( Table table : namespace.getTables() ) {
			if ( table.isPhysicalTable()
					&& !table.isView()
					&& schemaFilter.includeTable( table )
					&& inclusionFilter.matches( table ) ) {
				checkExportIdentifier( table, exportIdentifiers);
				applySqlStrings(
						dialect.getTableExporter().getSqlDropStrings( table, metadata, context),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void dropUserDefinedTypes(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				final List<UserDefinedType> dependencyOrderedUserDefinedTypes = namespace.getDependencyOrderedUserDefinedTypes();
				Collections.reverse( dependencyOrderedUserDefinedTypes );
				for ( UserDefinedType userDefinedType : dependencyOrderedUserDefinedTypes ) {
					applySqlStrings(
							dialect.getUserDefinedTypeExporter()
									.getSqlDropStrings( userDefinedType, metadata, context ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private static void dropSchemasAndCatalogs(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			Dialect dialect,
			Formatter formatter,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		boolean tryToDropCatalogs = options.shouldManageNamespaces() && dialect.canCreateCatalog();
		boolean tryToDropSchemas = options.shouldManageNamespaces() && dialect.canCreateSchema();
		if ( tryToDropCatalogs || tryToDropSchemas) {
			final Set<Identifier> exportedCatalogs = new HashSet<>();
			for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
				if ( schemaFilter.includeNamespace( namespace ) ) {
					Namespace.Name logicalName = namespace.getName();
					Namespace.Name physicalName = namespace.getPhysicalName();

					if ( tryToDropSchemas ) {
						final Identifier schemaPhysicalName = context.schemaWithDefault( physicalName.getSchema() );
						if ( schemaPhysicalName != null ) {
							final String schemaName = schemaPhysicalName.render( dialect );
							applySqlStrings( dialect.getDropSchemaCommand( schemaName ), formatter, options, targets);
						}
					}

					if (tryToDropCatalogs) {
						final Identifier catalogLogicalName = logicalName.getCatalog();
						final Identifier catalogPhysicalName = context.catalogWithDefault( physicalName.getCatalog() );
						if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName ) ) {
							final String catalogName = catalogPhysicalName.render( dialect );
							applySqlStrings( dialect.getDropCatalogCommand( catalogName ), formatter, options, targets );
							exportedCatalogs.add( catalogLogicalName );
						}
					}
				}
			}
		}
	}

	private static Collection<AuxiliaryDatabaseObject> reverse(Collection<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects) {
		final List<AuxiliaryDatabaseObject> list = new ArrayList<>( auxiliaryDatabaseObjects );
		Collections.reverse( list );
		return list;
	}

	private void applyConstraintDropping(
			Namespace namespace,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext context,
			ContributableMatcher inclusionFilter,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();
		if ( dialect.dropConstraints() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable()
						&& schemaFilter.includeTable( table )
						&& inclusionFilter.matches( table ) ) {
					for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
						applySqlStrings(
								dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, metadata, context ),
								formatter,
								options,
								targets
						);
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

	@Override
	public DelayedDropAction buildDelayedAction(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher inclusionFilter,
			SourceDescriptor sourceDescriptor) {
		final JournalingGenerationTarget target = new JournalingGenerationTarget();
		final Dialect dialect = tool.getServiceRegistry().requireService( JdbcEnvironment.class ).getDialect();
		doDrop( metadata, options, inclusionFilter, dialect, sourceDescriptor, target );
		return new DelayedDropActionImpl( target.commands, tool.getCustomDatabaseGenerationTarget() );
	}

	/**
	 * For tests
	 */
	public void doDrop(Metadata metadata, boolean manageNamespaces, GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry =
				( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
						.getServiceRegistry();
		doDrop(
				metadata,
				serviceRegistry,
				serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	/**
	 * For tests
	 */
	public void doDrop(
			Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map<String,Object> settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		if ( targets == null || targets.length == 0 ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( settings );
			targets = new GenerationTarget[] {
				new GenerationTargetToDatabase(
						serviceRegistry.requireService( TransactionCoordinatorBuilder.class )
								.buildDdlTransactionIsolator( jdbcContext ),
						true
				)
			};
		}

		doDrop(
				metadata,
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
				serviceRegistry.requireService( JdbcEnvironment.class ).getDialect(),
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

	private static class DelayedDropActionImpl implements DelayedDropAction, Serializable {
		private static final CoreMessageLogger log = CoreLogging.messageLogger( DelayedDropActionImpl.class );

		private final ArrayList<String> commands;
		private GenerationTarget target;

		public DelayedDropActionImpl(ArrayList<String> commands, GenerationTarget target) {
			this.commands = commands;
			this.target = target;
		}

		@Override
		public void perform(ServiceRegistry serviceRegistry) {
			log.startingDelayedSchemaDrop();

			final JdbcContext jdbcContext = new JdbcContextDelayedDropImpl( serviceRegistry );

			if ( target == null ) {
				target = new GenerationTargetToDatabase(
						serviceRegistry.requireService( TransactionCoordinatorBuilder.class )
								.buildDdlTransactionIsolator( jdbcContext ),
						true
				);
			}

			target.prepare();
			try {
				for ( String command : commands ) {
					try {
						target.accept( command );
					}
					catch (CommandAcceptanceException e) {
						// implicitly we do not "halt on error", but we do want to
						// report the problem
						log.unsuccessfulSchemaManagementCommand( command );
						log.debugf( e, "Error performing delayed DROP command [%s]", command );
					}
				}
			}
			finally {
				target.release();
			}
		}

		private static class JdbcContextDelayedDropImpl implements JdbcContext {
			private final ServiceRegistry serviceRegistry;
			private final JdbcServices jdbcServices;
			private final JdbcConnectionAccess jdbcConnectionAccess;

			public JdbcContextDelayedDropImpl(ServiceRegistry serviceRegistry) {
				this.serviceRegistry = serviceRegistry;
				this.jdbcServices = serviceRegistry.requireService( JdbcServices.class );
				this.jdbcConnectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
				if ( jdbcConnectionAccess == null ) {
					// todo : log or error?
					throw new SchemaManagementException(
							"Could not build JDBC Connection context to drop schema on SessionFactory close"
					);
				}
			}

			@Override
			public JdbcConnectionAccess getJdbcConnectionAccess() {
				return jdbcConnectionAccess;
			}

			@Override
			public Dialect getDialect() {
				return jdbcServices.getJdbcEnvironment().getDialect();
			}

			@Override
			public SqlStatementLogger getSqlStatementLogger() {
				return jdbcServices.getSqlStatementLogger();
			}

			@Override
			public SqlExceptionHelper getSqlExceptionHelper() {
				return jdbcServices.getSqlExceptionHelper();
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return serviceRegistry;
			}
		}
	}
}
