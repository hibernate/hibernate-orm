/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.tool.schema.internal.ColumnValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.type.BasicType;

class ExportableColumnHelper {

	static Column column(Database database, Table table, String segmentColumnName, BasicType<?> type, String typeName) {
		final var column = new Column( segmentColumnName );
		column.setSqlType( typeName );
		column.setValue( new ColumnValue( database, table, column, type ) );
		return column;
	}
}
