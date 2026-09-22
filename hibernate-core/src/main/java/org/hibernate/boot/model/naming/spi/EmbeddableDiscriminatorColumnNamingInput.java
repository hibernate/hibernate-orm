/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a polymorphic embeddable discriminator column.
/// The attribute path is owner-relative, without synthetic element markers.
/// The default column name is the existing unquoted fallback for the effective
/// mapping source, before logical or physical naming.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record EmbeddableDiscriminatorColumnNamingInput(
		EntityNamingInput entity,
		String embeddableTypeName,
		String attributePath,
		Kind kind,
		String defaultColumnName) {
	public EmbeddableDiscriminatorColumnNamingInput {
		requireNonNull( entity, "entity" );
		requireNonNull( embeddableTypeName, "embeddableTypeName" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( kind, "kind" );
		requireNonNull( defaultColumnName, "defaultColumnName" );
	}

	/// The component's role at the naming site. Nested attributes inside a
	/// collection element use [#EMBEDDED_ATTRIBUTE].
	public enum Kind { EMBEDDED_ATTRIBUTE, COLLECTION_ELEMENT }
}
