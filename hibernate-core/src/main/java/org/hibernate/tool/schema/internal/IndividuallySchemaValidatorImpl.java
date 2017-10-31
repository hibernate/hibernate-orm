/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Andrea Boriero
 *
 * This implementation executes one {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
 * for each {@link javax.persistence.Entity} in order to determine if a corresponding database table exists.
 */
public class IndividuallySchemaValidatorImpl extends AbstractSchemaValidator {

	public IndividuallySchemaValidatorImpl(
			HibernateSchemaManagementTool tool,
			DatabaseModel databaseModel,
			SchemaFilter validateFilter) {
		super( tool, databaseModel, validateFilter );
	}

	@Override
	protected void validateTables(
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect,
			Namespace namespace) {
		for ( Table table : namespace.getTables() ) {
			if ( table.isExportable() ) {
				final ExportableTable exportableTable = (ExportableTable) table;
				if ( schemaFilter.includeTable( exportableTable ) ) {
					final TableInformation tableInformation = databaseInformation.getTableInformation(
							exportableTable.getQualifiedTableName()
					);
					validateTable( exportableTable, tableInformation, options, dialect );
				}
			}
		}
	}
}
