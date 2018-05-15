/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

/**
 * This is functionally nothing more than the creation script from the older SchemaExport class (plus some
 * additional stuff in the script).
 *
 * @author Steve Ebersole
 */
public class SchemaDropperImpl implements SchemaDropper {
	private static final Logger log = Logger.getLogger( SchemaDropperImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;
	private final DatabaseModel databaseModel;
	private final JdbcServices jdbcServices;


	public SchemaDropperImpl(HibernateSchemaManagementTool tool, DatabaseModel databaseModel) {
		this( tool, databaseModel, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaDropperImpl(
			HibernateSchemaManagementTool tool,
			DatabaseModel databaseModel,
			SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter;
		this.databaseModel = databaseModel;
		this.jdbcServices = tool.getServiceRegistry().getService( JdbcServices.class );

	}

	public SchemaDropperImpl(DatabaseModel databaseModel, ServiceRegistry serviceRegistry) {
		this( databaseModel, serviceRegistry, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaDropperImpl(DatabaseModel databaseModel, ServiceRegistry serviceRegistry, SchemaFilter schemaFilter) {
		SchemaManagementTool smt = serviceRegistry.getService( SchemaManagementTool.class );
		if ( smt == null || !HibernateSchemaManagementTool.class.isInstance( smt ) ) {
			smt = new HibernateSchemaManagementTool();
			( (HibernateSchemaManagementTool) smt ).injectServices( (ServiceRegistryImplementor) serviceRegistry );
		}
		this.tool = (HibernateSchemaManagementTool) smt;
		this.schemaFilter = schemaFilter;
		this.databaseModel = databaseModel;
		this.jdbcServices = tool.getServiceRegistry().getService( JdbcServices.class );
	}

	@Override
	public void doDrop(
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {

		if ( targetDescriptor.getTargetTypes().isEmpty() ) {
			return;
		}

		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
		final GenerationTarget[] targets = tool.buildGenerationTargets( targetDescriptor, jdbcContext, options.getConfigurationValues(), true );

		doDrop( options, jdbcContext.getDialect(), sourceDescriptor, targets );
	}

	public void doDrop(
			ExecutionOptions options,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performDrop( options, dialect, sourceDescriptor, targets );
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
			ExecutionOptions options,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		final ImportSqlCommandExtractor commandExtractor = tool.getServiceRegistry().getService( ImportSqlCommandExtractor.class );
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		if ( sourceDescriptor.getSourceType() == SourceType.SCRIPT ) {
			dropFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
		}
		else if ( sourceDescriptor.getSourceType() == SourceType.METADATA ) {
			dropFromCreationContext( options, dialect, formatter, targets );
		}
		else if ( sourceDescriptor.getSourceType() == SourceType.METADATA_THEN_SCRIPT ) {
			dropFromCreationContext( options, dialect, formatter, targets );
			dropFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
		}
		else {
			dropFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
			dropFromCreationContext( options, dialect, formatter, targets );
		}
	}

	private void dropFromScript(
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

	private void dropFromCreationContext(
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		boolean tryToDropCatalogs = false;
		boolean tryToDropSchemas = false;
		if ( options.shouldManageNamespaces() ) {
			if ( dialect.canCreateSchema() ) {
				tryToDropSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToDropCatalogs = true;
			}
		}

		final Set<String> exportIdentifiers = new HashSet<>( 50 );

		// NOTE : init commands are irrelevant for dropping...

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				continue;
			}
			applySqlStrings(
					dialect.getAuxiliaryDatabaseObjectExporter()
							.getSqlDropStrings( auxiliaryDatabaseObject, jdbcServices ),
					formatter,
					options,
					targets
			);
		}

		for ( Namespace namespace : databaseModel.getNamespaces() ) {

			if ( !schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			// we need to drop all constraints/indexes prior to dropping the tables
			applyConstraintDropping( namespace, formatter, options, targets );

			// now it's safe to drop the tables
			namespace.getTables().stream()
					.filter( table -> table.isExportable() )
					.filter( table -> schemaFilter.includeTable( (ExportableTable) table ) )
					.map( table -> (ExportableTable) table )
					.forEach( table -> {
						checkExportIdentifier( table, exportIdentifiers );
						applySqlStrings(
								dialect.getTableExporter()
										.getSqlDropStrings( table, jdbcServices ),
								formatter,
								options,
								targets
						);
					} );

			for ( Sequence sequence : namespace.getSequences() ) {
				if ( !schemaFilter.includeSequence( sequence ) ) {
					continue;
				}
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings(
						dialect.getSequenceExporter().getSqlDropStrings( sequence, jdbcServices ),
						formatter,
						options,
						targets
				);
			}
		}

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				continue;
			}
			applySqlStrings(
					auxiliaryDatabaseObject.getSqlDropStrings(),
					formatter,
					options,
					targets
			);
		}

		if ( tryToDropCatalogs || tryToDropSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<>();

			for ( Namespace namespace : databaseModel.getNamespaces() ) {

				if ( !schemaFilter.includeNamespace( namespace ) ) {
					continue;
				}

				if ( tryToDropSchemas && namespace.getSchemaName() != null ) {
					applySqlStrings(
							dialect.getDropSchemaCommand(
									namespace.getSchemaName().render( dialect )
							),
							formatter,
							options,
							targets
					);
				}
				if ( tryToDropCatalogs ) {
					final Identifier cataloglName = namespace.getCatalogName();

					if ( cataloglName != null && !exportedCatalogs.contains( cataloglName ) ) {
						applySqlStrings(
								dialect.getDropCatalogCommand(
										cataloglName.render( dialect )
								),
								formatter,
								options,
								targets
						);
						exportedCatalogs.add( cataloglName );
					}
				}
			}
		}
	}

	private void applyConstraintDropping(
			Namespace namespace,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final Dialect dialect = jdbcServices.getDialect();

		if ( !dialect.dropConstraints() ) {
			return;
		}

		for ( Table table : namespace.getTables() ) {
			if ( !table.isExportable() ) {
				continue;
			}
			if ( !schemaFilter.includeTable( (ExportableTable) table ) ) {
				continue;
			}

			for ( ForeignKey foreignKey : table.getForeignKeys() ) {
				applySqlStrings(
						dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, jdbcServices ),
						formatter,
						options,
						targets
				);
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

		String sqlStringFormatted = formatter.format( sqlString );
		for ( GenerationTarget target : targets ) {
			try {
				target.accept( sqlStringFormatted );
			}
			catch (CommandAcceptanceException e) {
				options.getExceptionHandler().handleException( e );
			}
		}
	}

	/**
	 * For testing...
	 *
	 *
	 * @return The generation commands
	 */
	public List<String> generateDropCommands(final boolean manageNamespaces) {
		final JournalingGenerationTarget target = new JournalingGenerationTarget();

		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
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

		dropFromCreationContext( options, dialect, FormatStyle.NONE.getFormatter(), target );

		return target.commands;
	}

	@Override
	public DelayedDropAction buildDelayedAction(
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor) {
		final JournalingGenerationTarget target = new JournalingGenerationTarget();
		doDrop(
				options,
				tool.getServiceRegistry().getService( JdbcEnvironment.class ).getDialect(),
				sourceDescriptor,
				target
		);
		return new DelayedDropActionImpl( target.commands );
	}

	/**
	 * For tests
	 */
	public void doDrop(boolean manageNamespaces, GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
		doDrop(
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	/**
	 * For tests
	 */
	public void doDrop(
			final Map settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
		if ( targets == null || targets.length == 0 ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( settings );
			targets = new GenerationTarget[] {
				new GenerationTargetToDatabase(
						serviceRegistry
								.getService( TransactionCoordinatorBuilder.class ).buildDdlTransactionIsolator( jdbcContext ),
						true
				)
			};
		}

		doDrop(
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
				serviceRegistry.getService( JdbcEnvironment.class ).getDialect(),
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

		public DelayedDropActionImpl(ArrayList<String> commands) {
			this.commands = commands;
		}

		@Override
		public void perform(ServiceRegistry serviceRegistry) {
			log.startingDelayedSchemaDrop();

			final JdbcContext jdbcContext = new JdbcContextDelayedDropImpl( serviceRegistry );
			final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
					serviceRegistry.getService( TransactionCoordinatorBuilder.class ).buildDdlTransactionIsolator( jdbcContext ),
					true
			);

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

		private class JdbcContextDelayedDropImpl implements JdbcContext {
			private final ServiceRegistry serviceRegistry;
			private final JdbcServices jdbcServices;
			private final JdbcConnectionAccess jdbcConnectionAccess;

			public JdbcContextDelayedDropImpl(ServiceRegistry serviceRegistry) {
				this.serviceRegistry = serviceRegistry;
				this.jdbcServices = serviceRegistry.getService( JdbcServices.class );
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
