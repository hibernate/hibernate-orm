/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for ListIndexColumn naming.
/// Attribute paths are relative to the declaring entity or collection.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ListIndexColumnNamingInput(String attributePath) {
	public ListIndexColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
