/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming an aggregate container, distinct from its scalar members.
/// The path is owner-relative; the attribute name is the existing terminal fallback.
/// Scope describes where the value is stored, not the container for its own members.
///
/// All reference components are non-null.
///
/// @param entity The owning entity's naming information
/// @param embeddableTypeName The mapped aggregate type's name
/// @param attributePath The full owner-relative path, without synthetic map-key markers
/// @param attributeName The terminal attribute name used by supplied strategies
/// @param usage The role of this aggregate value at the naming site
/// @param storageKind The aggregate's storage format, excluding any SQL type name
/// @param plural Whether this container stores multiple aggregate values, rather than one;
/// independent of whether its mapping role is a collection element or map key
/// @param scope Whether the container occupies a table column or an enclosing aggregate member
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record AggregateColumnNamingInput(
		EntityNamingInput entity,
		String embeddableTypeName,
		String attributePath,
		String attributeName,
		Usage usage,
		StorageKind storageKind,
		boolean plural,
		Scope scope) {
	public AggregateColumnNamingInput {
		requireNonNull( entity, "entity" );
		requireNonNull( embeddableTypeName, "embeddableTypeName" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( attributeName, "attributeName" );
		requireNonNull( usage, "usage" );
		requireNonNull( storageKind, "storageKind" );
		requireNonNull( scope, "scope" );
	}

	/// Nested attributes use ATTRIBUTE even when declared inside a collection element or map key.
	public enum Usage {
		/// An aggregate attribute, including nested attributes inside elements or keys.
		ATTRIBUTE,
		/// The aggregate representing a collection element itself.
		COLLECTION_ELEMENT,
		/// The aggregate representing a map key itself.
		MAP_KEY
	}
	/// The aggregate representation; names SQL storage formats, not named SQL types.
	public enum StorageKind {
		/// Structured SQL storage.
		STRUCT,
		/// JSON storage.
		JSON,
		/// XML storage.
		XML
	}
	/// Whether the named container is a table column or a member of another aggregate.
	public enum Scope {
		/// A container materialized as a column of a table.
		TABLE_COLUMN,
		/// A nested container stored inside another aggregate.
		AGGREGATE_MEMBER
	}
}
