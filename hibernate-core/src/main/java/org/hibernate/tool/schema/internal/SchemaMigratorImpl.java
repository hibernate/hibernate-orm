/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.schema.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Schema;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.Target;


/**
 * @author Steve Ebersole
 */
public class SchemaMigratorImpl implements SchemaMigrator {
	@Override
	public void doMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			List<Target> targets) throws SchemaManagementException {

		for ( Target target : targets ) {
			target.prepare();
		}

		doMigrationToTargets( metadata, existingDatabase, createSchemas, targets );

		for ( Target target : targets ) {
			target.release();
		}
	}


	protected void doMigrationToTargets(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			List<Target> targets) {
		final Set<String> exportIdentifiers = new HashSet<String>( 50 );
		
		final Database database = metadata.getDatabase();

		for ( Schema schema : database.getSchemas() ) {
			if ( createSchemas ) {
				if ( schema.getName().getSchema() != null ) {
					if ( !existingDatabase.schemaExists( schema.getName() ) ) {
						applySqlString(
								database.getJdbcEnvironment().getDialect().getCreateSchemaCommand(
										schema.getName().getSchema().render( database.getJdbcEnvironment().getDialect() )
								),
								targets,
								false
						);
					}
				}
			}

			for ( Table table : schema.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
				if ( tableInformation == null ) {
					createTable( table, metadata, targets );
				}
				else {
					migrateTable( table, tableInformation, targets, metadata );
				}
			}

			for ( Table table : schema.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}

				final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
				if ( tableInformation == null ) {
					// big problem...
					throw new SchemaManagementException( "BIG PROBLEM" );
				}

				applyIndexes( table, tableInformation, metadata, targets );
				applyUniqueKeys( table, tableInformation, metadata, targets );
				applyForeignKeys( table, tableInformation, metadata, targets );
			}

			for ( Sequence sequence : schema.getSequences() ) {
				checkExportIdentifier( sequence, exportIdentifiers );
				final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getName() );
				if ( sequenceInformation != null ) {
					// nothing we really can do...
					continue;
				}

				applySqlStrings(
						database.getJdbcEnvironment().getDialect().getSequenceExporter().getSqlCreateStrings(
								sequence,
								metadata
						),
						targets,
						false
				);
			}
		}
	}

	private void createTable(Table table, Metadata metadata, List<Target> targets) {
		applySqlStrings(
				metadata.getDatabase().getDialect().getTableExporter().getSqlCreateStrings( table, metadata ),
				targets,
				false
		);
	}

	private void migrateTable(
			Table table,
			TableInformation tableInformation,
			List<Target> targets,
			Metadata metadata) {
		final Database database = metadata.getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		//noinspection unchecked
		applySqlStrings(
				table.sqlAlterStrings(
						dialect,
						metadata,
						tableInformation,
						getDefaultCatalogName( database ),
						getDefaultSchemaName( database )
				),
				targets,
				false
		);
	}

	private void applyIndexes(Table table, TableInformation tableInformation, Metadata metadata, List<Target> targets) {
		final Exporter<Index> exporter = metadata.getDatabase().getJdbcEnvironment().getDialect().getIndexExporter();

		final Iterator<Index> indexItr = table.getIndexIterator();
		while ( indexItr.hasNext() ) {
			final Index index = indexItr.next();
			if ( StringHelper.isEmpty( index.getName() ) ) {
				continue;
			}

			final IndexInformation existingIndex = findMatchingIndex( index, tableInformation );
			if ( existingIndex != null ) {
				continue;
			}

			applySqlStrings(
					exporter.getSqlCreateStrings( index, metadata ),
					targets,
					false
			);
		}
	}

	private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
		return tableInformation.getIndex( Identifier.toIdentifier( index.getName() ) );
	}

	private UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy;

	private void applyUniqueKeys(Table table, TableInformation tableInfo, Metadata metadata, List<Target> targets) {
		if ( uniqueConstraintStrategy == null ) {
			uniqueConstraintStrategy = determineUniqueConstraintSchemaUpdateStrategy( metadata );
		}

		if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			return;
		}

		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();
		final Exporter<Constraint> exporter = dialect.getUniqueKeyExporter();

		final Iterator ukItr = table.getUniqueKeyIterator();
		while ( ukItr.hasNext() ) {
			final UniqueKey uniqueKey = (UniqueKey) ukItr.next();
			// Skip if index already exists. Most of the time, this
			// won't work since most Dialects use Constraints. However,
			// keep it for the few that do use Indexes.
			if ( tableInfo != null && StringHelper.isNotEmpty( uniqueKey.getName() ) ) {
				final IndexInformation indexInfo = tableInfo.getIndex( Identifier.toIdentifier( uniqueKey.getName() ) );
				if ( indexInfo != null ) {
					continue;
				}
			}

			if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) {
				applySqlStrings(
						exporter.getSqlDropStrings( uniqueKey, metadata ),
						targets,
						true
				);
			}

			applySqlStrings(
					exporter.getSqlCreateStrings( uniqueKey, metadata ),
					targets,
					true
			);
		}
	}

	private UniqueConstraintSchemaUpdateStrategy determineUniqueConstraintSchemaUpdateStrategy(Metadata metadata) {
		final ConfigurationService cfgService = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ConfigurationService.class );

		return UniqueConstraintSchemaUpdateStrategy.interpret(
				cfgService.getSetting(
						AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY,
						StandardConverters.STRING
				)
		);
	}

	private void applyForeignKeys(
			Table table,
			TableInformation tableInformation,
			Metadata metadata,
			List<Target> targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();
		if ( !dialect.hasAlterTable() ) {
			return;
		}

		final Exporter<ForeignKey> exporter = dialect.getForeignKeyExporter();

		@SuppressWarnings("unchecked")
		final Iterator<ForeignKey> fkItr = table.getForeignKeyIterator();
		while ( fkItr.hasNext() ) {
			final ForeignKey foreignKey = fkItr.next();
			if ( !foreignKey.isPhysicalConstraint() ) {
				continue;
			}

			if ( !foreignKey.isCreationEnabled() ) {
				continue;
			}

			final ForeignKeyInformation existingForeignKey = findMatchingForeignKey( foreignKey, tableInformation );

			// todo : shouldn't we just drop+recreate if FK exists?
			//		this follows the existing code from legacy SchemaUpdate which just skipped

			if ( existingForeignKey == null ) {
				// in old SchemaUpdate code, this was the trigger to "create"
				applySqlStrings(
						exporter.getSqlCreateStrings( foreignKey, metadata ),
						targets,
						false
				);
			}
		}
	}

	private ForeignKeyInformation findMatchingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		if ( foreignKey.getName() == null ) {
			return null;
		}
		return tableInformation.getForeignKey( Identifier.toIdentifier( foreignKey.getName() ) );
	}

	private void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format(
							"Export identifier [%s] encountered more than once",
							exportIdentifier
					)
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	private static void applySqlStrings(String[] sqlStrings, List<Target> targets, boolean quiet) {
		if ( sqlStrings == null ) {
			return;
		}

		for ( String sqlString : sqlStrings ) {
			applySqlString( sqlString, targets, quiet );
		}
	}

	private static void applySqlString(String sqlString, List<Target> targets, boolean quiet) {
		if ( sqlString == null ) {
			return;
		}

		for ( Target target : targets ) {
			try {
				target.accept( sqlString );
			}
			catch (SchemaManagementException e) {
				if ( !quiet ) {
					throw e;
				}
				// otherwise ignore the exception
			}
		}
	}

	private static void applySqlStrings(Iterator<String> sqlStrings, List<Target> targets, boolean quiet) {
		if ( sqlStrings == null ) {
			return;
		}

		while ( sqlStrings.hasNext() ) {
			final String sqlString = sqlStrings.next();
			applySqlString( sqlString, targets, quiet );
		}
	}

	private String getDefaultCatalogName(Database database) {
		final Identifier identifier = database.getDefaultSchema().getPhysicalName().getCatalog();
		return identifier == null ? null : identifier.render( database.getJdbcEnvironment().getDialect() );
	}

	private String getDefaultSchemaName(Database database) {
		final Identifier identifier = database.getDefaultSchema().getPhysicalName().getSchema();
		return identifier == null ? null : identifier.render( database.getJdbcEnvironment().getDialect() );
	}
}
