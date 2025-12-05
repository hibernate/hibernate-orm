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
 * This implementation executes one {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * for each {@link jakarta.persistence.Entity} in order to determine if a corresponding database table exists.
 */
public class IndividuallySchemaValidatorImpl extends AbstractSchemaValidator {

	public IndividuallySchemaValidatorImpl(
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
			Dialect dialect,
			Namespace namespace) {
		for ( var table : namespace.getTables() ) {
			if ( schemaFilter.includeTable( table )
					&& table.isPhysicalTable()
					&& contributableInclusionFilter.matches( table ) ) {
				final var tableInformation =
						databaseInformation.getTableInformation( table.getQualifiedTableName() );
				validateTable( table, tableInformation, metadata, options, dialect );
			}
		}
	}
}
