/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a collection or association-table column referencing its owner.
/// The reference describes the owner, not the collection element or association target.
///
/// All reference components are non-null.
///
/// @param owner The entity referenced by the owner-key columns
/// @param attributePath The collection or association path relative to the owner
/// @param inverseAttributePath The mapped inverse association path when available; for a one-to-many key this
/// currently contains the collection path itself. Empty when no such path is supplied
/// @param kind The mapping role of the owner key
/// @param reference The settled referenced owner table and ordered columns, with the column for this decision selected
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record CollectionKeyNamingInput(
		EntityNamingInput owner,
		String attributePath,
		java.util.Optional<String> inverseAttributePath,
		Kind kind,
		ReferencedColumnsNamingInput reference) {
	public CollectionKeyNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( inverseAttributePath, "inverseAttributePath" );
		requireNonNull( kind, "kind" );
		requireNonNull( reference, "reference" );
	}

	/// The mapping whose columns reference the owner.
	public enum Kind {
		/// Owner key in an element-collection table.
		ELEMENT_COLLECTION,
		/// Owner key in a plural association table.
		ASSOCIATION_TABLE,
		/// Owner key stored with a one-to-many association's target rows.
		ONE_TO_MANY,
		/// Owner key in a to-one association's join table.
		TO_ONE_TABLE
	}
}
