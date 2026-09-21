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

	public NamingNamePair column() {
		return columns.get( columnPosition );
	}
}
