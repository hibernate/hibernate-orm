/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;

/// Resolved index materialization input.
///
/// @param table The table that owns the index.
/// @param selectables The ordered index selectables.
/// @param columnNames The logical column/formula names used for implicit naming.
/// @param metadataBuildingContext The metadata-building context used for
/// 		implicit index naming.
/// @param sourceRole Human-readable role used in diagnostics.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedIndex(
		@Nonnull Table table,
		@Nonnull List<Selectable> selectables,
		@Nonnull List<String> columnNames,
		@Nonnull MetadataBuildingContext metadataBuildingContext,
		@Nullable String name,
		boolean unique,
		@Nullable String type,
		@Nullable String using,
		@Nullable String options,
		@Nullable List<String> columnOrderings,
		@Nullable String sourceRole) {
	public ResolvedIndex {
		selectables = List.copyOf( selectables );
		columnNames = List.copyOf( columnNames );
		if ( columnOrderings != null ) {
			columnOrderings = java.util.Collections.unmodifiableList( new java.util.ArrayList<>( columnOrderings ) );
		}
	}

	public static ResolvedIndex explicit(
			Table table,
			List<Selectable> selectables,
			List<String> columnNames,
			MetadataBuildingContext metadataBuildingContext,
			@Nullable String name,
			boolean unique,
			@Nullable String type,
			@Nullable String using,
			@Nullable String options,
			@Nullable List<String> columnOrderings,
			@Nullable String sourceRole) {
		return new ResolvedIndex(
				table,
				selectables,
				columnNames,
				metadataBuildingContext,
				name,
				unique,
				type,
				using,
				options,
				columnOrderings,
				sourceRole
		);
	}
}
