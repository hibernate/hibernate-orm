/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * This implementation executes one {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * for each {@link jakarta.persistence.Entity} in order to determine if a corresponding database table exists.
 *
 * @author Andrea Boriero
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
		final var tablesInformation =
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
			for ( var table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table )
						&& table.isPhysicalTable()
						&& contributableInclusionFilter.matches( table ) ) {
					checkExportIdentifier( table, exportIdentifiers );
					final var tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
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

			for ( var table : namespace.getTables() ) {
				if ( schemaFilter.includeTable( table )
						&& table.isPhysicalTable()
						&& contributableInclusionFilter.matches( table ) ) {
					final var tableInformation = tablesInformation.getTableInformation( table );
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
