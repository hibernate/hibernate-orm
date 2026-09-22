/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a polymorphic embeddable discriminator column.
/// The attribute path is owner-relative, without synthetic element markers.
/// Declaration origin describes the effective mapping source after override precedence.
/// Nonempty explicit names bypass implicit naming. The strategy computes the default.
///
/// All reference components are non-null.
///
/// @param entity The owning mapped entity, including for a collection element
/// @param embeddableTypeName The mapped embeddable type name; does not require loading its Java class
/// @param attributePath The full owner-relative path, such as `home.pet` or `pets`, without synthetic element markers
/// @param kind Whether the polymorphic component is an embedded attribute or the collection element itself
/// @param declaration The effective column declaration after override precedence. A nonempty name bypasses
/// the callback, so a present declaration here has an empty name. This is source information,
/// not a precomputed discriminator name
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record EmbeddableDiscriminatorColumnNamingInput(
		EntityNamingInput entity,
		String embeddableTypeName,
		String attributePath,
		Kind kind,
		Declaration declaration) {
	public EmbeddableDiscriminatorColumnNamingInput {
		requireNonNull( entity, "entity" );
		requireNonNull( embeddableTypeName, "embeddableTypeName" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( kind, "kind" );
		requireNonNull( declaration, "declaration" );
	}

	/// Origin of the effective discriminator column declaration, including empty names.
	public enum Declaration {
		/// No effective discriminator-column declaration or override.
		ABSENT,
		/// An effective discriminator-column declaration, with no overriding column source.
		DISCRIMINATOR_COLUMN,
		/// A discriminator-column override taking precedence over the type declaration.
		OVERRIDE
	}

	/// The component's role at the naming site. Nested attributes inside a
	/// collection element use [#EMBEDDED_ATTRIBUTE].
	public enum Kind {
		/// An embedded attribute, including one nested inside a collection element.
		EMBEDDED_ATTRIBUTE,
		/// The polymorphic component representing the collection element itself.
		COLLECTION_ELEMENT
	}
}
