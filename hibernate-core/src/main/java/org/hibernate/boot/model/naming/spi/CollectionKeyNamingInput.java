/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a collection or association-table owner key. The inverse path is present only for a mapped inverse association.
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

	public enum Kind { ELEMENT_COLLECTION, ASSOCIATION_TABLE, ONE_TO_MANY, TO_ONE_TABLE }
}
