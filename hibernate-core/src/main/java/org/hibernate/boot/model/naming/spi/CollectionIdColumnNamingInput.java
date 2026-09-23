/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable naming facts for a collection-row identifier column.
/// The nonempty attribute path is relative to the mapped owning entity.
/// The table describes the actual destination, with settled dependency names.
///
/// All reference components are non-null.
///
/// @param owner The mapped entity owning the collection, rather than a mapped superclass declaring it
/// @param attributePath The nonempty collection path relative to the owner, for example `details.items`
/// @param table The actual destination table of the collection identifier, with settled dependency names
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record CollectionIdColumnNamingInput(
		EntityNamingInput owner,
		String attributePath,
		TableNamingInput table) {
	public CollectionIdColumnNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( table, "table" );
		if ( attributePath.isEmpty() ) {
			throw new IllegalArgumentException( "Collection attribute path must not be empty" );
		}
	}
}
