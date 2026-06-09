/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.mapping.Collection;

import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

/// Local state for a collection table whose key depends on the owner identifier.
///
/// Collection binding can create the collection table, element/index values, and
/// table annotations before the owner identifier is needed to create the
/// collection key.  The table-key phase consumes this record to create the
/// dependent key, call the mapping model's key-creation hooks, and apply table
/// indexes and unique constraints.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollectionTableBinding(
		Collection collection,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource,
		UniqueConstraint[] uniqueConstraints,
		Index[] indexes) {
}
