/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Settled referenced table and ordered columns for one join-column decision.
/// Column position is zero-based in the resolved join order.
///
/// All reference components are non-null.
///
/// @param table The settled table containing the referenced columns; may be an inline view
/// @param columns The nonempty sequence of selected logical/physical column-name pairs in resolved join
/// correspondence order, not necessarily annotation declaration order. Copied to an immutable list;
/// null entries are rejected. Each logical name is the selected reference alias or declaration
/// @param columnPosition The zero-based position in `columns` of the column referenced by this naming decision
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ReferencedColumnsNamingInput(
		TableNamingInput table,
		java.util.List<NamingNamePair> columns,
		int columnPosition) {
	public ReferencedColumnsNamingInput {
		requireNonNull( table, "table" );
		columns = java.util.List.copyOf( columns );
		if ( columnPosition < 0 || columnPosition >= columns.size() ) {
			throw new IllegalArgumentException( "Join column position is outside the referenced columns" );
		}
	}

	/// The referenced column selected for the current local join-column decision.
	///
	/// @return The pair at [#columnPosition()] in [#columns()]
	public NamingNamePair column() {
		return columns.get( columnPosition );
	}
}
