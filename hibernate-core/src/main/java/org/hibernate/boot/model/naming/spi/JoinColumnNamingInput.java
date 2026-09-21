/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a to-one association column, including association identifiers.
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
