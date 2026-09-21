/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for naming an association table between two entity tables.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record AssociationTableNamingInput(
		EntityNamingInput owner,
		TableNamingInput owningTable,
		EntityNamingInput target,
		TableNamingInput targetTable,
		String attributePath) {
	public AssociationTableNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( owningTable, "owningTable" );
		requireNonNull( target, "target" );
		requireNonNull( targetTable, "targetTable" );
		requireNonNull( attributePath, "attributePath" );
	}
}
