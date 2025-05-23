/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY;
import static org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy.SKIP;
import static org.hibernate.tool.schema.internal.SchemaCreatorImpl.createUserDefinedTypes;
import static org.hibernate.tool.schema.internal.SchemaDropperImpl.dropUserDefinedTypes;

/**
 * Base implementation of {@link SchemaMigrator}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaMigrator implements SchemaMigrator {
	private static final Logger log = Logger.getLogger( IndividuallySchemaMigratorImpl.class );

	protected HibernateSchemaManagementTool tool;
	protected SchemaFilter schemaFilter;
	private UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy;

	public AbstractSchemaMigrator(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter == null ? DefaultSchemaFilter.INSTANCE : schemaFilter;
	}

	@Override
	public void doMigration(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor) {
		final SqlStringGenerationContext sqlGenerationContext = sqlGenerationContext( metadata, options );
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
			final DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext );
			try {
				final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
						tool.getServiceRegistry(),
						ddlTransactionIsolator,
						sqlGenerationContext,
						tool
				);

				final GenerationTarget[] targets = tool.buildGenerationTargets(
						targetDescriptor,
						ddlTransactionIsolator,
						options.getConfigurationValues()
				);

				try {
					for ( GenerationTarget target : targets ) {
						target.prepare();
					}

					try {
						performMigration(
								metadata,
								databaseInformation,
								options,
								contributableInclusionFilter,
								jdbcContext.getDialect(),
								sqlGenerationContext,
								targets
						);
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
				finally {
					try {
						databaseInformation.cleanup();
					}
					catch (Exception e) {
						log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
					}
				}
			}
			finally {
				ddlTransactionIsolator.release();
			}
		}
	}

	private SqlStringGenerationContext sqlGenerationContext(Metadata metadata, ExecutionOptions options) {
		return SqlStringGenerationContextImpl.fromConfigurationMapForMigration(
				tool.getServiceRegistry().requireService( JdbcEnvironment.class ),
				metadata.getDatabase(),
				options.getConfigurationValues()
		);
	}

	protected abstract NameSpaceTablesInformation performTablesMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget[] targets);

	private void performMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		final Set<String> exportIdentifiers = CollectionHelper.setOfSize( 50 );

		final Database database = metadata.getDatabase();
		Exporter<AuxiliaryDatabaseObject> auxiliaryExporter = dialect.getAuxiliaryDatabaseObjectExporter();

		// Drop all AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryExporter.getSqlDropStrings( auxiliaryDatabaseObject, metadata, sqlGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}

		// Drop all UDTs
		dropUserDefinedTypes( metadata, options, schemaFilter, dialect, formatter, sqlGenerationContext, targets );

		// Create before-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation()
					&& auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryExporter.getSqlCreateStrings( auxiliaryDatabaseObject, metadata, sqlGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}

		// Recreate all UDTs
		createUserDefinedTypes( metadata, options, schemaFilter, dialect, formatter, sqlGenerationContext, targets );

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
		final Map<Namespace, NameSpaceTablesInformation> tablesInformation = new HashMap<>();
		final Set<Identifier> exportedCatalogs = new HashSet<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			final NameSpaceTablesInformation nameSpaceTablesInformation = performTablesMigration(
					metadata,
					existingDatabase,
					options,
					contributableInclusionFilter,
					dialect,
					formatter,
					exportIdentifiers,
					tryToCreateCatalogs,
					tryToCreateSchemas,
					exportedCatalogs,
					namespace,
					sqlGenerationContext, targets
			);
			tablesInformation.put( namespace, nameSpaceTablesInformation );
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Sequence sequence : namespace.getSequences() ) {
					if ( contributableInclusionFilter.matches( sequence ) ) {
						checkExportIdentifier( sequence, exportIdentifiers);
						final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getName() );
						if ( sequenceInformation == null ) {
							applySequence( sequence, dialect, metadata, formatter, options, sqlGenerationContext, targets );
						}
					}
				}
			}
		}

		//NOTE : Foreign keys must be created *after* all tables of all namespaces for cross namespace fks. see HHH-10420
		for ( Namespace namespace : database.getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				final NameSpaceTablesInformation nameSpaceTablesInformation = tablesInformation.get( namespace );
				for ( Table table : namespace.getTables() ) {
					if ( schemaFilter.includeTable( table ) && contributableInclusionFilter.matches( table ) ) {
						final TableInformation tableInformation = nameSpaceTablesInformation.getTableInformation( table );
						if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
							applyForeignKeys( table, tableInformation, dialect, metadata, formatter, options,
										sqlGenerationContext, targets );
						}
					}
				}
			}
		}

		// Create after-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() && auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryExporter.getSqlCreateStrings( auxiliaryDatabaseObject, metadata, sqlGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void applySequence(
			Sequence sequence,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getSequenceExporter().getSqlCreateStrings( sequence, metadata, sqlGenerationContext ),
				formatter,
				options,
				targets
		);
	}

	protected void createTable(
			Table table,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableExporter().getSqlCreateStrings( table, metadata, sqlGenerationContext ),
				formatter,
				options,
				targets
		);
	}

	protected void migrateTable(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableMigrator()
						.getSqlAlterStrings( table, metadata, tableInformation, sqlGenerationContext ),
				formatter,
				options,
				targets
		);
	}

	protected void applyIndexes(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		final Exporter<Index> exporter = dialect.getIndexExporter();
		for ( Index index : table.getIndexes().values() ) {
			if ( !isEmpty( index.getName() ) ) {
				IndexInformation existingIndex = null;
				if ( tableInformation != null ) {
					existingIndex = findMatchingIndex( index, tableInformation );
				}
				if ( existingIndex == null ) {
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( index, metadata, sqlGenerationContext ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
		return tableInformation.getIndex( Identifier.toIdentifier( index.getName() ) );
	}

	protected void applyUniqueKeys(
			Table table,
			TableInformation tableInfo,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		if ( uniqueConstraintStrategy == null ) {
			uniqueConstraintStrategy = determineUniqueConstraintSchemaUpdateStrategy();
		}

		if ( uniqueConstraintStrategy != SKIP ) {
			final Exporter<UniqueKey> exporter = dialect.getUniqueKeyExporter();
			for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
				// Skip if index already exists. Most of the time, this
				// won't work since most Dialects use Constraints. However,
				// keep it for the few that do use Indexes.
				IndexInformation indexInfo = null;
				if ( tableInfo != null && StringHelper.isNotEmpty( uniqueKey.getName() ) ) {
					indexInfo = tableInfo.getIndex( Identifier.toIdentifier( uniqueKey.getName() ) );
				}
				if ( indexInfo == null ) {
					if ( uniqueConstraintStrategy == DROP_RECREATE_QUIETLY ) {
						applySqlStrings(
								true,
								exporter.getSqlDropStrings( uniqueKey, metadata, sqlGenerationContext ),
								formatter,
								options,
								targets
						);
					}

					applySqlStrings(
							true,
							exporter.getSqlCreateStrings( uniqueKey, metadata, sqlGenerationContext ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private UniqueConstraintSchemaUpdateStrategy determineUniqueConstraintSchemaUpdateStrategy() {
		final String updateStrategy =
				tool.getServiceRegistry().requireService( ConfigurationService.class )
						.getSetting( UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, STRING );
		return UniqueConstraintSchemaUpdateStrategy.interpret( updateStrategy );
	}

	protected void applyForeignKeys(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlGenerationContext,
			GenerationTarget... targets) {
		if ( dialect.hasAlterTable() ) {
			final Exporter<ForeignKey> exporter = dialect.getForeignKeyExporter();
			for ( ForeignKey foreignKey : table.getForeignKeyCollection() ) {
				if ( foreignKey.isPhysicalConstraint()
						&& foreignKey.isCreationEnabled()
						&& ( tableInformation == null || !checkForExistingForeignKey( foreignKey, tableInformation ) ) ) {
					// todo : shouldn't we just drop+recreate if FK exists?
					//		this follows the existing code from legacy SchemaUpdate which just skipped
					// in old SchemaUpdate code, this was the trigger to "create"
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( foreignKey, metadata, sqlGenerationContext ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	/**
	 * Check if the ForeignKey already exists. First check based on definition and if that is not matched check if a key
	 * with the exact same name exists. Keys with the same name are presumed to be functional equal.
	 *
	 * @param foreignKey - ForeignKey, new key to be created
	 * @param tableInformation - TableInformation, information of existing keys
	 * @return boolean, true if key already exists
	 */
	private boolean checkForExistingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		if ( foreignKey.getName() == null || tableInformation == null ) {
			return false;
		}
		else {
			final String referencingColumn = foreignKey.getColumn( 0 ).getName();
			final String referencedTable = foreignKey.getReferencedTable().getName();
			// Find existing keys based on referencing column and referencedTable. "referencedColumnName"
			// is not checked because that always is the primary key of the "referencedTable".
			return equivalentForeignKeyExistsInDatabase( tableInformation, referencingColumn, referencedTable )
				// And finally just compare the name of the key. If a key with the same name exists we
				// assume the function is also the same...
				|| tableInformation.getForeignKey( Identifier.toIdentifier( foreignKey.getName() ) ) != null;
		}
	}

	boolean equivalentForeignKeyExistsInDatabase(TableInformation tableInformation, String referencingColumn, String referencedTable) {
		return StreamSupport.stream( tableInformation.getForeignKeys().spliterator(), false )
				.flatMap( foreignKeyInformation -> StreamSupport.stream( foreignKeyInformation.getColumnReferenceMappings().spliterator(), false ) )
				.anyMatch( columnReferenceMapping -> {
			final ColumnInformation referencingColumnMetadata = columnReferenceMapping.getReferencingColumnMetadata();
			final ColumnInformation referencedColumnMetadata = columnReferenceMapping.getReferencedColumnMetadata();
			final String existingReferencingColumn = referencingColumnMetadata.getColumnIdentifier().getText();
			final String existingReferencedTable =
					referencedColumnMetadata.getContainingTableInformation().getName().getTableName().getCanonicalName();
			return referencingColumn.equalsIgnoreCase( existingReferencingColumn )
				&& referencedTable.equalsIgnoreCase( existingReferencedTable );
		} );
	}

	protected void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format("Export identifier [%s] encountered more than once", exportIdentifier )
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	protected static void applySqlStrings(
			boolean quiet,
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings != null ) {
			for ( String sql : sqlStrings ) {
				applySqlString( quiet, sql, formatter, options, targets );
			}
		}
	}

	protected void createSchemaAndCatalog(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs, Namespace namespace,
			SqlStringGenerationContext context,
			GenerationTarget[] targets) {
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			Namespace.Name logicalName = namespace.getName();
			Namespace.Name physicalName = namespace.getPhysicalName();

			if ( tryToCreateCatalogs ) {
				final Identifier catalogLogicalName = logicalName.catalog();
				final Identifier catalogPhysicalName = context.catalogWithDefault( physicalName.catalog() );
				if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName )
						&& !existingDatabase.catalogExists( catalogPhysicalName ) ) {
					applySqlStrings(
							false,
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
				if ( schemaPhysicalName != null && !existingDatabase.schemaExists( physicalName ) ) {
					applySqlStrings(
							false,
							dialect.getCreateSchemaCommand( schemaPhysicalName.render( dialect ) ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private static void applySqlString(
			boolean quiet,
			String sql,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( !isEmpty( sql ) ) {
			final String formattedSql = formatter.format( sql );
			for ( GenerationTarget target : targets ) {
				try {
					target.accept( formattedSql );
				}
				catch (CommandAcceptanceException e) {
					if ( !quiet ) {
						options.getExceptionHandler().handleException( e );
					}
					// otherwise ignore the exception
				}
			}
		}
	}
}
