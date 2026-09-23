/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a column referencing an entity-valued map key.
///
/// All reference components are non-null.
///
/// @param owner The entity owning the map
/// @param keyEntity The entity type used as the map key
/// @param attributePath The map attribute path relative to the owner
/// @param reference The settled key-entity table and ordered referenced columns, with the column for this decision selected
/// @param referencesPrimaryKey Whether the referenced columns represent the key entity's primary key rather than a non-primary-key target
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record MapKeyJoinColumnNamingInput(
		EntityNamingInput owner,
		EntityNamingInput keyEntity,
		String attributePath,
		ReferencedColumnsNamingInput reference,
		boolean referencesPrimaryKey) {
	public MapKeyJoinColumnNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( keyEntity, "keyEntity" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( reference, "reference" );
	}
}
