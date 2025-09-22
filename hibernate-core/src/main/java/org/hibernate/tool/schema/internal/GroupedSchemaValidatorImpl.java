/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Andrea Boriero
 *
 * This implementation executes a single {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * to retrieve all the database table in order to determine if all the {@link jakarta.persistence.Entity} have a mapped database tables.
 */
public class GroupedSchemaValidatorImpl extends AbstractSchemaValidator {

	public GroupedSchemaValidatorImpl(
			HibernateSchemaManagementTool tool,
			SchemaFilter validateFilter) {
		super( tool, validateFilter );
	}

	@Override
	protected void validateTables(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect, Namespace namespace) {

		final var tables = databaseInformation.getTablesInformation( namespace );
		for ( var table : namespace.getTables() ) {
			if ( schemaFilter.includeTable( table )
					&& table.isPhysicalTable()
					&& contributableInclusionFilter.matches( table ) ) {
				validateTable(
						table,
						tables.getTableInformation( table ),
						metadata,
						options,
						dialect
				);
			}
		}
	}
}
