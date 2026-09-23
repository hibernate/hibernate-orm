/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a to-one association column, including association identifiers.
///
/// All reference components are non-null.
///
/// @param owner The entity declaring the to-one association or association identifier
/// @param target The entity referenced by the association
/// @param attributePath The association path relative to the owner, including embeddable nesting
/// @param reference The settled referenced table and ordered target columns, with the column for this decision selected
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record JoinColumnNamingInput(
		EntityNamingInput owner,
		EntityNamingInput target,
		String attributePath,
		ReferencedColumnsNamingInput reference) {
	public JoinColumnNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( target, "target" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( reference, "reference" );
	}
}
