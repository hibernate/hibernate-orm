/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/// Side-car foreign-key creation state based on ordered selectable
/// correspondences.
///
/// This is intentionally not a replacement for the mapping model's key APIs.
/// It gives the new binder pipeline a way to create a physical constraint from
/// paired local/referenced columns without relying on later positional sorting
/// of the owning [SimpleValue].
///
/// @param table The "key table" for the foreign-key.
/// @param foreignKeyName The explicit/source-provided name for the foreign-key, or {@code null}
/// 		when Hibernate should generate the implicit name later. This is not the final resolved name,
/// 		which might be [implicit][org.hibernate.boot.model.naming.ImplicitNamingStrategy#determineForeignKeyName].
/// @param referencedEntityName The mapped entity name referenced by the foreign-key.
/// @param foreignKeyDefinition The source-provided foreign-key DDL definition, or {@code null} when no custom definition was declared.
/// @param foreignKeyOptions Source-provided foreign-key DDL options, or {@code null} when no custom options were declared.
/// @param onDeleteAction The configured ON DELETE action, or {@code null} when no ON DELETE action was declared.
/// @param selectableOrder The already-resolved local-to-target column correspondence.  This is a snapshot taken
/// 		after the relevant identifier, attribute, and table-key ordering decisions have settled.
///
/// @since 9.0
/// @author Steve Ebersole
record ResolvedForeignKey(
		@Nonnull Table table,
		@Nullable String foreignKeyName,
		@Nonnull String referencedEntityName,
		@Nullable String foreignKeyDefinition,
		@Nullable String foreignKeyOptions,
		@Nullable OnDeleteAction onDeleteAction,
		@Nonnull SelectableOrderResolution selectableOrder) {
	static ResolvedForeignKey from(
			SimpleValue value,
			@Nonnull String referencedEntityName,
			@Nonnull SelectableOrderResolution selectableOrder) {
		return new ResolvedForeignKey(
				value.getTable(),
				value.getForeignKeyName(),
				referencedEntityName,
				value.getForeignKeyDefinition(),
				value.getForeignKeyOptions(),
				value.getOnDeleteAction(),
				selectableOrder
		);
	}

	ForeignKey createForeignKey(PersistentClass referencedEntity) {
		if ( selectableOrder.isEmpty()
				|| referencedEntity.getRootClass().isAuxiliaryColumnInPrimaryKey() ) {
			return null;
		}

		final ForeignKey foreignKey = table.createForeignKey(
				foreignKeyName,
				selectableOrder.foreignKeyColumnMappings(),
				referencedEntityName,
				foreignKeyDefinition,
				foreignKeyOptions
		);
		foreignKey.setOnDeleteAction( onDeleteAction );
		return foreignKey;
	}
}
