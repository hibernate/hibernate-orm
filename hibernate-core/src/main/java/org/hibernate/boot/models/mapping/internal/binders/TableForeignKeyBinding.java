/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import org.hibernate.boot.models.mapping.internal.sources.ForeignKeySource;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;

/// Pending foreign-key binding for a table key value.
///
/// Joined-subclass tables, secondary tables, association tables, and collection
/// tables all use dependent key values whose columns are copied from another
/// identifier.  Their physical foreign-key constraints are created in the
/// foreign-key phase after all table keys have been built and after source
/// metadata such as `@ForeignKey` can be applied uniformly.
///
/// @since 9.0
/// @author Steve Ebersole
public record TableForeignKeyBinding(
		PersistentClass ownerBinding,
		KeyValue key,
		String referencedEntityName,
		ForeignKeySource foreignKeySource,
		ResolvedForeignKey resolvedForeignKey) {
	public TableForeignKeyBinding(
			PersistentClass ownerBinding,
			KeyValue key,
			String referencedEntityName,
			ForeignKeySource foreignKeySource) {
		this( ownerBinding, key, referencedEntityName, foreignKeySource, null );
	}
}
