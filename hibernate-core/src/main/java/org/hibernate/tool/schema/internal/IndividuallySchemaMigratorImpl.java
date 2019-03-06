/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Andrea Boriero
 *
 * This implementation executes one {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * for each {@link javax.persistence.Entity} in order to determine if a corresponding database table exists.
 */
public class IndividuallySchemaMigratorImpl extends AbstractSchemaMigrator {

	public IndividuallySchemaMigratorImpl(
			HibernateSchemaManagementTool tool,
			SchemaFilter schemaFilter) {
		super( tool, schemaFilter );
	}

	@Override
	protected NameSpaceTablesInformation performTablesMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace,
			GenerationTarget[] targets) {
		final NameSpaceTablesInformation tablesInformation =
				new NameSpaceTablesInformation( metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper() );

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
			for ( Table table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
					checkExportIdentifier( table, exportIdentifiers );
					final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
					if ( tableInformation == null ) {
						createTable( table, dialect, metadata, formatter, options, targets );
					}
					else if ( tableInformation.isPhysicalTable() ) {
						tablesInformation.addTableInformation( tableInformation );
						migrateTable( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
				}
			}

			for ( Table table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
					final TableInformation tableInformation = tablesInformation.getTableInformation( table );
					if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
						applyIndexes( table, tableInformation, dialect, metadata, formatter, options, targets );
						applyUniqueKeys( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
				}
			}
		}
		return tablesInformation;
	}
}
