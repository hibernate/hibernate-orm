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
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.ExecutionOptions;

/**
 * @author Andrea Boriero
 */
public class SchemaValidatorImpl extends AbstractSchemaValidator {
	public SchemaValidatorImpl(){
	}

	@Override
	protected void validateTables(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect,
			Namespace namespace) {
		for ( Table table : namespace.getTables() ) {
			if ( schemaFilter.includeTable( table ) && table.isPhysicalTable() ) {
				final TableInformation tableInformation = databaseInformation.getTableInformation(
						table.getQualifiedTableName()
				);
				validateTable( table, tableInformation, metadata, options, dialect );
			}
		}
	}
}
