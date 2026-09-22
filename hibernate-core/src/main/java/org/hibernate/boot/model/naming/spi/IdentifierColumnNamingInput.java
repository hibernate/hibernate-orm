/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming an entity identifier column.
/// Attribute paths are relative to the declaring entity or collection.
///
/// All reference components are non-null.
///
/// @param entity The entity whose identifier is being named
/// @param attributePath The identifier attribute path supplied by the binding site, without an entity-name prefix
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record IdentifierColumnNamingInput(EntityNamingInput entity, String attributePath) {
	public IdentifierColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( entity, "entity" );
	}
}
