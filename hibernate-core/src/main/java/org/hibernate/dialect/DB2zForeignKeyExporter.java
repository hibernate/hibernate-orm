/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;

public class DB2zForeignKeyExporter extends StandardForeignKeyExporter {

	public DB2zForeignKeyExporter(Dialect dialect) {
		super( dialect );
	}

	@Override
	protected void appendDefaultOnDeleteAction(ForeignKey foreignKey, Metadata metadata, StringBuilder buffer) {
		// DB2z requires that self-referential foreign key constraints explicitly define the delete action
		// https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statements-alter-table#db2z_sql_altertable__title__68
		if ( foreignKey.getTable() == foreignKey.getReferencedTable() ) {
			buffer.append( " on delete no action" );
		}
	}
}
