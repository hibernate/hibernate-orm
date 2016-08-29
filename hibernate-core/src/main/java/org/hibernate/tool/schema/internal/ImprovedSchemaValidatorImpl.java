/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.spi.ExecutionOptions;

/**
 * @author Andrea Boriero
 */
public class ImprovedSchemaValidatorImpl extends AbstractSchemaValidator {

	public ImprovedSchemaValidatorImpl(){
	}

	@Override
	protected void validateTables(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect, Namespace namespace) {

		final NameSpaceTablesInformation tables = databaseInformation.getTablesInformation( namespace );
		for ( Table table : namespace.getTables() ) {
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
