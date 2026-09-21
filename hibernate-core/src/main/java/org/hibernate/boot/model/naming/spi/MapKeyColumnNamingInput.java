/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for MapKeyColumn naming.
/// Attribute paths are relative to the declaring entity or collection.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record MapKeyColumnNamingInput(String attributePath) {
	public MapKeyColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
