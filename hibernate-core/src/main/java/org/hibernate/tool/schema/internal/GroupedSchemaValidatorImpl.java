/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Andrea Boriero
 *
 * This implementation executes a single {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * to retrieve all the database table in order to determine if all the {@link javax.persistence.Entity} have a mapped database tables.
 */
public class GroupedSchemaValidatorImpl extends AbstractSchemaValidator {

	public GroupedSchemaValidatorImpl(
			HibernateSchemaManagementTool tool,
			RuntimeModelCreationContext modelCreationContext,
			SchemaFilter validateFilter) {
		super( tool, validateFilter );
	}

	@Override
	protected void validateTables(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect, MappedNamespace namespace) {

		final NameSpaceTablesInformation tables = databaseInformation.getTablesInformation( namespace );
		for ( MappedTable table : namespace.getTables() ) {
			if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
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
