/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for naming a table owned by a collection or association attribute.
///
/// All reference components are non-null.
///
/// @param owner The entity owning the collection or association
/// @param owningTable The settled table dependency for that entity
/// @param attributePath The collection or association path relative to the owner, including embeddable nesting
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record CollectionTableNamingInput(
		EntityNamingInput owner,
		TableNamingInput owningTable,
		String attributePath) {
	public CollectionTableNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( owningTable, "owningTable" );
		requireNonNull( attributePath, "attributePath" );
	}
}
