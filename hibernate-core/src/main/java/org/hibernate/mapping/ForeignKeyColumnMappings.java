/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Internal;

/// Ordered foreign-key column correspondences.
///
/// This is a snapshot used at foreign-key creation time, not a live view over
/// mutable boot [Value] column lists.  Callers must create it after the relevant
/// identifier, attribute, table-key, and referenced-column ordering decisions
/// have settled.
///
/// @since 9.0
@Internal
public record ForeignKeyColumnMappings(List<ForeignKeyColumnMapping> mappings) implements Serializable {
	public ForeignKeyColumnMappings {
		mappings = List.copyOf( mappings );
	}
}
