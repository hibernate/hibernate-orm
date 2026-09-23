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
/// @param columns The key columns, in the sequence they should appear in the
/// 		unique-key definition.
/// @param metadataBuildingContext The metadata-building context used for
/// 		implicit key naming.
/// @param columnOrderings Optional per-column SQL ordering fragments, such as
/// 		{@code asc} or {@code desc}, aligned by position with {@code columns}.
/// 		A {@code null} list, or a {@code null} entry, means no ordering fragment
/// 		was specified for that column.
/// @param sourceRole Human-readable role used in diagnostics.
///
/// @param columnReferences Optional unresolved annotation references, resolved after column binding
/// @param declarationLocation Location of a source `@UniqueConstraint`, or null for other UK sources.
/// @param indexSource Whether this candidate originated from a unique index declaration
/// @param entityName Declaring entity for attribute fallback, or null for other contributors
/// @param collectionRole Declaring collection role for explicit collection references, or null
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
		@Nullable String sourceRole,
		@Nullable List<String> columnReferences,
		@Nullable String declarationLocation,
		boolean indexSource,
		@Nullable String entityName,
		@Nullable String collectionRole) {
	public ResolvedUniqueKey(Table table, List<Column> columns, MetadataBuildingContext context,
			String name, boolean nameExplicit, boolean explicit, String options,
			List<String> orderings, boolean nullsNotDistinct, boolean tableUniqueKey, String sourceRole) {
		this( table, columns, context, name, nameExplicit, explicit, options, orderings,
				nullsNotDistinct, tableUniqueKey, sourceRole, null, null, false, null, null );
	}

	/// Retain annotation references until column binding is complete (HHH-20917).
	public static ResolvedUniqueKey references(Table table, List<String> references,
			MetadataBuildingContext context, String name, String options, List<String> orderings, String role) {
		return new ResolvedUniqueKey( table, List.of(), context, name, name != null && !name.isEmpty(),
				true, options, orderings, false, true, role, references, null, false, null, null );
	}

	/// Retain the declaration location for duplicate explicit-name validation (HHH-20918).
	public static ResolvedUniqueKey uniqueConstraint(Table table, List<String> references,
			MetadataBuildingContext context, String name, String options, String location, String entityName, String collectionRole) {
		return new ResolvedUniqueKey( table, List.of(), context, name, name != null && !name.isEmpty(),
				true, options, null, false, true, location, references, location, false, entityName, collectionRole );
	}

	/// Preserve unique-index provenance while using the UK finalization lifecycle.
	public static ResolvedUniqueKey index(ResolvedIndex index, List<Column> columns, List<String> orderings) {
		return new ResolvedUniqueKey( index.table(), columns, index.metadataBuildingContext(), index.name(),
				index.name() != null, true, index.options(), orderings, false, true, index.sourceRole(), null, null, true, index.entityName(), index.collectionRole() );
	}

	public ResolvedUniqueKey {
		if ( columnReferences != null ) { columnReferences = List.copyOf( columnReferences ); }
		columns = List.copyOf( columns );
		if ( columnOrderings != null ) {
			columnOrderings = java.util.Collections.unmodifiableList( new java.util.ArrayList<>( columnOrderings ) );
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
				value.getColumnContainer().requireTable(),
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
