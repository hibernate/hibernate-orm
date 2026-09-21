/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming an any discriminator or key column.
/// Attribute paths are relative to the declaring entity or collection.
/// The position is zero-based within the declared key-column sequence; the
/// discriminator uses position zero. There is no single referenced target table.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record AnyColumnNamingInput(String attributePath, int columnPosition) {
	public AnyColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
		if ( columnPosition < 0 ) {
			throw new IllegalArgumentException( "Negative any-column position" );
		}
	}
}
