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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
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
		if ( smt == null || !HibernateSchemaManagementTool.class.isInstance( smt ) ) {
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
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {

		if ( targetDescriptor.getTargetTypes().isEmpty() ) {
			return;
		}

		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
		final GenerationTarget[] targets = tool.buildGenerationTargets( targetDescriptor, jdbcContext, options.getConfigurationValues(), true );

		doDrop( metadata, options, jdbcContext.getDialect(), sourceDescriptor, targets );
	}

	public void doDrop(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			SourceDescriptor sourceDescriptor,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performDrop( metadata, options, dialect, sourceDescriptor, targets );
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
			dropFromMetadata( metadata, options, dialect, formatter, targets );
		}
		else if ( sourceDescriptor.getSourceType() == SourceType.METADATA_THEN_SCRIPT ) {
			dropFromMetadata( metadata, options, dialect, formatter, targets );
			dropFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
		}
		else {
			dropFromScript( sourceDescriptor.getScriptSourceInput(), commandExtractor, formatter, options, targets );
			dropFromMetadata( metadata, options, dialect, formatter, targets );
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

	private void dropFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		final Database database = metadata.getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

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

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		// NOTE : init commands are irrelevant for dropping...

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}
			if ( !auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				continue;
			}

			applySqlStrings(
					dialect.getAuxiliaryDatabaseObjectExporter().getSqlDropStrings( auxiliaryDatabaseObject, metadata ),
					formatter,
					options,
					targets
			);
		}

		for ( Namespace namespace : database.getNamespaces() ) {

			if ( !schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			// we need to drop all constraints/indexes prior to dropping the tables
			applyConstraintDropping( namespace, metadata, formatter, options, targets );

			// now it's safe to drop the tables
			for ( Table table : namespace.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}
				if ( !schemaFilter.includeTable( table ) ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings( dialect.getTableExporter().getSqlDropStrings( table, metadata ), formatter, options,targets );
			}

			for ( Sequence sequence : namespace.getSequences() ) {
				if ( !schemaFilter.includeSequence( sequence ) ) {
					continue;
				}
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings( dialect.getSequenceExporter().getSqlDropStrings( sequence, metadata ), formatter, options, targets );
			}
		}

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}
			if ( !auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				continue;
			}

			applySqlStrings(
					auxiliaryDatabaseObject.sqlDropStrings( jdbcEnvironment.getDialect() ),
					formatter,
					options,
					targets
			);
		}

		if ( tryToDropCatalogs || tryToDropSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<Identifier>();

			for ( Namespace namespace : database.getNamespaces() ) {

				if ( !schemaFilter.includeNamespace( namespace ) ) {
					continue;
				}

				if ( tryToDropSchemas && namespace.getPhysicalName().getSchema() != null ) {
					applySqlStrings(
							dialect.getDropSchemaCommand(
									namespace.getPhysicalName().getSchema().render( dialect )
							),
							formatter,
							options,
							targets
					);
				}
				if ( tryToDropCatalogs ) {
					final Identifier catalogLogicalName = namespace.getName().getCatalog();
					final Identifier catalogPhysicalName = namespace.getPhysicalName().getCatalog();

					if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName ) ) {
						applySqlStrings(
								dialect.getDropCatalogCommand(
										catalogPhysicalName.render( dialect )
								),
								formatter,
								options,
								targets
						);
						exportedCatalogs.add( catalogLogicalName );
					}
				}
			}
		}
	}

	private void applyConstraintDropping(
			Namespace namespace,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		if ( !dialect.dropConstraints() ) {
			return;
		}

		for ( Table table : namespace.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}
			if ( !schemaFilter.includeTable( table ) ) {
				continue;
			}

			final Iterator fks = table.getForeignKeyIterator();
			while ( fks.hasNext() ) {
				final ForeignKey foreignKey = (ForeignKey) fks.next();
				applySqlStrings(
						dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, metadata ),
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

		for ( GenerationTarget target : targets ) {
			try {
				target.accept( formatter.format( sqlString ) );
			}
			catch (CommandAcceptanceException e) {
				options.getExceptionHandler().handleException( e );
			}
		}
	}

	/**
	 * For testing...
	 *
	 * @param metadata The metadata for which to generate the creation commands.
	 *
	 * @return The generation commands
	 */
	public List<String> generateDropCommands(Metadata metadata, final boolean manageNamespaces) {
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

		dropFromMetadata( metadata, options, dialect, FormatStyle.NONE.getFormatter(), target );

		return target.commands;
	}

	@Override
	public DelayedDropAction buildDelayedAction(
			Metadata metadata,
			ExecutionOptions options,
			SourceDescriptor sourceDescriptor) {
		final JournalingGenerationTarget target = new JournalingGenerationTarget();
		doDrop( metadata, options, tool.getServiceRegistry().getService( JdbcEnvironment.class ).getDialect(), sourceDescriptor, target );
		return new DelayedDropActionImpl( target.commands );
	}

	/**
	 * For tests
	 */
	public void doDrop(Metadata metadata, boolean manageNamespaces, GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();
		doDrop(
				metadata,
				serviceRegistry,
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
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
			final Map settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		if ( targets == null || targets.length == 0 ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( settings );
			targets = new GenerationTarget[] {
				new GenerationTargetToDatabase(
						serviceRegistry.getService( TransactionCoordinatorBuilder.class ).buildDdlTransactionIsolator( jdbcContext ),
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
