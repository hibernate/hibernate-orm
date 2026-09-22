/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a list index column.
/// Attribute paths are relative to the declaring entity or collection.
///
/// All reference components are non-null.
///
/// @param attributePath The list attribute path supplied by the declaring binding; identifies the list, not its index column
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ListIndexColumnNamingInput(String attributePath) {
	public ListIndexColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
