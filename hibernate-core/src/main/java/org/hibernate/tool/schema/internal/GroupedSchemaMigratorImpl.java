/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Andrea Boriero
 *
 * This implementation executes a single {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * to retrieve all the database table in order to determine if all the {@link javax.persistence.Entity} have a mapped database tables.
 */
public class GroupedSchemaMigratorImpl extends AbstractSchemaMigrator {

	public GroupedSchemaMigratorImpl(
			HibernateSchemaManagementTool tool,
			DatabaseModel databaseModel,
			SchemaFilter schemaFilter) {
		super( tool, databaseModel, schemaFilter );
	}

	@Override
	protected NameSpaceTablesInformation performTablesMigration(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace, GenerationTarget[] targets) {
		final NameSpaceTablesInformation tablesInformation =
				new NameSpaceTablesInformation( databaseModel.getJdbcEnvironment().getIdentifierHelper() );

		if ( schemaFilter.includeNamespace( namespace ) ) {
			createSchemaAndCatalog(
					existingDatabase,
					options,
					dialect,
					formatter,
					tryToCreateCatalogs,
					tryToCreateSchemas,
					exportedCatalogs,
					namespace,
					targets
			);
			final NameSpaceTablesInformation tables = existingDatabase.getTablesInformation( namespace );
			for ( Table table : namespace.getTables() ) {
				if ( table.isExportable() ) {
					final ExportableTable exportableTable = (ExportableTable) table;
					if ( schemaFilter.includeTable( exportableTable ) ) {
						checkExportIdentifier( exportableTable, exportIdentifiers );
						final TableInformation tableInformation = tables.getTableInformation( exportableTable );
						if ( tableInformation == null ) {
							createTable( exportableTable, dialect, formatter, options, targets );
						}
						else if ( tableInformation.isPhysicalTable() ) {
							tablesInformation.addTableInformation( tableInformation );
							migrateTable( exportableTable, tableInformation, dialect, formatter, options, targets );
						}
					}
				}
			}

			for ( Table table : namespace.getTables() ) {
				if ( table.isExportable() ) {
					final ExportableTable exportableTable = (ExportableTable) table;
					if ( schemaFilter.includeTable( exportableTable ) ) {
						final TableInformation tableInformation = tablesInformation.getTableInformation( exportableTable );
						if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
							applyIndexes( exportableTable, tableInformation, dialect, formatter, options, targets );
							applyUniqueKeys( exportableTable, tableInformation, dialect, formatter, options, targets );
						}
					}
				}
			}
		}
		return tablesInformation;
	}
}
