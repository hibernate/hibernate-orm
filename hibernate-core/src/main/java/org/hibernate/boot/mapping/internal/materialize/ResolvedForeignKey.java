/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.mapping.internal.binders.SelectableOrderResolution;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/// Resolved foreign-key materialization input based on ordered selectable
/// correspondences.
///
/// This product gives materialization a physical constraint shape without
/// relying on later positional sorting of the owning [SimpleValue].
///
/// @param table The key table for the foreign key.
/// @param foreignKeyName The explicit/source-provided foreign-key name, or
/// 		{@code null} when Hibernate should generate the implicit name later.
/// @param referencedEntityName The mapped entity name referenced by the foreign
/// 		key.
/// @param foreignKeyDefinition The source-provided foreign-key DDL definition,
/// 		or {@code null} when none was declared.
/// @param foreignKeyOptions Source-provided foreign-key DDL options, or
/// 		{@code null} when none were declared.
/// @param onDeleteAction The configured ON DELETE action, or {@code null}.
/// @param selectableOrder The resolved local-to-target column correspondence.
/// @param referencedTable The referenced table, when the FK targets a
/// 		non-primary table of the referenced entity.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedForeignKey(
		@Nonnull Table table,
		@Nullable String foreignKeyName,
		@Nonnull String referencedEntityName,
		@Nullable String foreignKeyDefinition,
		@Nullable String foreignKeyOptions,
		@Nullable OnDeleteAction onDeleteAction,
		@Nonnull SelectableOrderResolution selectableOrder,
		@Nullable Table referencedTable) {
	public static ResolvedForeignKey from(
			SimpleValue value,
			@Nonnull String referencedEntityName,
			@Nonnull SelectableOrderResolution selectableOrder) {
		return from( value, referencedEntityName, selectableOrder, null );
	}

	public static ResolvedForeignKey from(
			SimpleValue value,
			@Nonnull String referencedEntityName,
			@Nonnull SelectableOrderResolution selectableOrder,
			@Nullable Table referencedTable) {
		return new ResolvedForeignKey(
				value.getTable(),
				value.getForeignKeyName(),
				referencedEntityName,
				value.getForeignKeyDefinition(),
				value.getForeignKeyOptions(),
				value.getOnDeleteAction(),
				selectableOrder,
				referencedTable
		);
	}
}
