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
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * This implementation executes a single {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * to retrieve all the database table in order to determine if all the {@link jakarta.persistence.Entity} have a mapped database tables.
 *
 * @author Andrea Boriero
 */
public class GroupedSchemaMigratorImpl extends AbstractSchemaMigrator {

	public GroupedSchemaMigratorImpl(
			HibernateSchemaManagementTool tool,
			SchemaFilter schemaFilter) {
		super( tool, schemaFilter );
	}

	@Override
	protected NameSpaceTablesInformation performTablesMigration(
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
			SqlStringGenerationContext context,
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
					context,
					targets
			);

			final NameSpaceTablesInformation tables = existingDatabase.getTablesInformation( namespace );
			for ( Table table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table )
						&& table.isPhysicalTable()
						&& contributableInclusionFilter.matches( table ) ) {
					checkExportIdentifier( table, exportIdentifiers );
					final TableInformation tableInformation = tables.getTableInformation( table );
					if ( tableInformation == null ) {
						createTable( table, dialect, metadata, formatter, options, context, targets );
					}
					else if ( tableInformation.isPhysicalTable() ) {
						tablesInformation.addTableInformation( tableInformation );
						migrateTable( table, tableInformation, dialect, metadata, formatter, options,
								context, targets );
					}
				}
			}

			for ( Table table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table )
						&& table.isPhysicalTable()
						&& contributableInclusionFilter.matches( table ) ) {
					final TableInformation tableInformation = tablesInformation.getTableInformation( table );
					if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
						applyIndexes( table, tableInformation, dialect, metadata, formatter, options,
								context, targets );
						applyUniqueKeys( table, tableInformation, dialect, metadata, formatter, options,
								context, targets );
					}
				}
			}
		}
		return tablesInformation;
	}
}
