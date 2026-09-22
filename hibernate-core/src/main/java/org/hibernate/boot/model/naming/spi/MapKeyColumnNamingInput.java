/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a basic map-key column.
/// Attribute paths are relative to the declaring entity or collection.
///
/// All reference components are non-null.
///
/// @param attributePath The map attribute path supplied by the declaring binding; identifies the map, not a synthetic key property
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record MapKeyColumnNamingInput(String attributePath) {
	public MapKeyColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
