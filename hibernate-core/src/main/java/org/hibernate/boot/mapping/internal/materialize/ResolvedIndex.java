/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.relational.naming.spi.LogicalName;

/// An index declaration retained until its table columns are available.
///
/// @param table Owning physical table
/// @param logicalTableName Selected source identity, captured before source binding is discarded
/// @param columnList Original declaration, parsed without rewriting expression text
/// @param metadataBuildingContext Mapping context used for resolution and naming
/// @param name Optional explicit index name
/// @param unique Source unique flag
/// @param type Optional index type
/// @param using Optional indexing method
/// @param options Optional export SQL options
/// @param sourceRole Class/member, containing annotation, and index declaration position
/// @param entityName Declaring entity for attribute-path fallback, or null for collection/join-table declarations
/// @param collectionRole Declaring collection role for explicit collection references, or null
/// @author Steve Ebersole
public record ResolvedIndex(
		@Nonnull PhysicalTable table,
		@Nonnull LogicalName logicalTableName,
		@Nonnull String columnList,
		@Nonnull MetadataBuildingContext metadataBuildingContext,
		@Nullable String name,
		boolean unique,
		@Nullable String type,
		@Nullable String using,
		@Nullable String options,
		@Nonnull String sourceRole,
		@Nullable String entityName,
		@Nullable String collectionRole) {
}
