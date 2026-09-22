/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a basic collection-element column.
/// Attribute paths are relative to the declaring entity or collection.
///
/// All reference components are non-null.
///
/// @param attributePath The collection attribute path supplied by the declaring binding; identifies the collection,
/// not a synthetic `element` property or the element type
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record CollectionElementColumnNamingInput(String attributePath) {
	public CollectionElementColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
