/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/// Resolved unique-key materialization input.
///
/// @param table The table that owns the unique key.
/// @param columns The ordered key columns.
/// @param metadataBuildingContext The metadata-building context used for
/// 		implicit key naming.
/// @param sourceRole Human-readable role used in diagnostics.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedUniqueKey(
		@Nonnull Table table,
		@Nonnull List<Column> columns,
		@Nonnull MetadataBuildingContext metadataBuildingContext,
		@Nullable String name,
		boolean nameExplicit,
		boolean explicit,
		@Nullable String options,
		@Nullable List<String> columnOrderings,
		boolean nullsNotDistinct,
		boolean tableUniqueKey,
		@Nullable String sourceRole) {
	public ResolvedUniqueKey {
		columns = List.copyOf( columns );
		if ( columnOrderings != null ) {
			columnOrderings = List.copyOf( columnOrderings );
		}
	}

	public static ResolvedUniqueKey from(
			SimpleValue value,
			MetadataBuildingContext metadataBuildingContext,
			String sourceRole) {
		if ( value.hasFormula() ) {
			throw new MappingException( "Unique key constraint involves formulas" );
		}
		return new ResolvedUniqueKey(
				value.getTable(),
				value.getConstraintColumns(),
				metadataBuildingContext,
				null,
				false,
				false,
				null,
				null,
				false,
				false,
				sourceRole
		);
	}

	public static ResolvedUniqueKey from(Column column, Table table, MetadataBuildingContext metadataBuildingContext) {
		return new ResolvedUniqueKey(
				table,
				List.of( column ),
				metadataBuildingContext,
				null,
				false,
				false,
				null,
				null,
				false,
				false,
				null
		);
	}

	public static ResolvedUniqueKey explicit(
			Table table,
			List<Column> columns,
			MetadataBuildingContext metadataBuildingContext,
			@Nullable String name,
			boolean nameExplicit,
			@Nullable String options,
			@Nullable List<String> columnOrderings,
			String sourceRole) {
		return new ResolvedUniqueKey(
				table,
				columns,
				metadataBuildingContext,
				name,
				nameExplicit,
				true,
				options,
				columnOrderings,
				false,
				true,
				sourceRole
		);
	}

	public static ResolvedUniqueKey named(
			Table table,
			List<Column> columns,
			MetadataBuildingContext metadataBuildingContext,
			@Nullable String name,
			@Nullable String sourceRole) {
		return new ResolvedUniqueKey(
				table,
				columns,
				metadataBuildingContext,
				name,
				false,
				false,
				null,
				null,
				false,
				true,
				sourceRole
		);
	}

	public static ResolvedUniqueKey internal(
			Table table,
			List<Column> columns,
			MetadataBuildingContext metadataBuildingContext,
			boolean nullsNotDistinct,
			@Nullable String sourceRole) {
		return new ResolvedUniqueKey(
				table,
				columns,
				metadataBuildingContext,
				null,
				false,
				false,
				null,
				null,
				nullsNotDistinct,
				true,
				sourceRole
		);
	}
}
