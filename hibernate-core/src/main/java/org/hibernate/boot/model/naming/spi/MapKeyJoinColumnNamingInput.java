/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a column referencing an entity-valued map key.
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
