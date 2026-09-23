/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import java.util.List;
import java.util.Objects;

/// A foreign-key source declaration, resolved against JDBC-discovered tables later.
/// Names retain source quoting; no naming strategy or system comparison policy is applied here.
///
/// @author Steve Ebersole
public record ForeignKeyDefinition(
		String name,
		TableIdentifier table,
		TableIdentifier referencedTable,
		List<ColumnReference> columns) {
	public ForeignKeyDefinition {
		Objects.requireNonNull( table );
		Objects.requireNonNull( referencedTable );
		columns = List.copyOf( columns );
	}

	/// An ordered pair of source column names, including any explicit quoting.
	public record ColumnReference(String column, String referencedColumn) {
		public ColumnReference {
			if ( column == null || column.isBlank() || referencedColumn == null || referencedColumn.isBlank() ) {
				throw new IllegalArgumentException( "A foreign-key column reference requires both column names" );
			}
		}
	}
}
