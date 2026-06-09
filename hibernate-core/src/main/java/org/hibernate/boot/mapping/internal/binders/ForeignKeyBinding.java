/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;

import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.ToOne;

/// Pending foreign-key binding for an association value.
///
/// Member binding creates association values and columns, but the physical
/// foreign-key constraint is a cross-value concern.  Recording it as typed state
/// lets a later phase create and customize constraints after table keys,
/// collection indexes, and inverse association structures have settled.
///
/// @since 9.0
/// @author Steve Ebersole
public record ForeignKeyBinding(
		PersistentClass ownerBinding,
		ToOne value,
		ForeignKeySource foreignKeySource,
		ResolvedForeignKey resolvedForeignKey,
		List<String> referencedColumnNames) {
	public ForeignKeyBinding {
		referencedColumnNames = List.copyOf( referencedColumnNames );
	}

	public ForeignKeyBinding(
			PersistentClass ownerBinding,
			ToOne value,
			ForeignKeySource foreignKeySource,
			ResolvedForeignKey resolvedForeignKey) {
		this( ownerBinding, value, foreignKeySource, resolvedForeignKey, List.of() );
	}

	public ForeignKeyBinding(
			PersistentClass ownerBinding,
			ToOne value,
			ForeignKeySource foreignKeySource,
			List<String> referencedColumnNames) {
		this( ownerBinding, value, foreignKeySource, null, referencedColumnNames );
	}

	public ForeignKeyBinding(
			PersistentClass ownerBinding,
			ToOne value,
			ForeignKeySource foreignKeySource) {
		this( ownerBinding, value, foreignKeySource, null, List.of() );
	}
}
