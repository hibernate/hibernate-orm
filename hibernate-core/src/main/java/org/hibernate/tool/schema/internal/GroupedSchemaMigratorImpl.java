/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Set;

import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
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
			SchemaFilter schemaFilter,
			RuntimeModelCreationContext modelCreationContext) {
		super( tool, schemaFilter, modelCreationContext );
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
				new NameSpaceTablesInformation( modelCreationContext.getDatabaseModel().getJdbcEnvironment().getIdentifierHelper() );

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
				if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
					checkExportIdentifier( table, exportIdentifiers );
					final TableInformation tableInformation = tables.getTableInformation( table );
					if ( tableInformation == null ) {
						createTable( table, dialect, metadata, formatter, options, targets );
					}
					else if ( tableInformation != null && tableInformation.isPhysicalTable() ) {
						tablesInformation.addTableInformation( tableInformation );
						migrateTable( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
				}
			}

			for ( Table table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
					final TableInformation tableInformation = tablesInformation.getTableInformation( table );
					if ( tableInformation == null || ( tableInformation != null && tableInformation.isPhysicalTable() ) ) {
						applyIndexes( table, tableInformation, dialect, metadata, formatter, options, targets );
						applyUniqueKeys( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
				}
			}
		}
		return tablesInformation;
	}
}
