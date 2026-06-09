/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.Internal;

/// Paired local and referenced columns used to create a foreign key without
/// losing correspondence through independent positional lists.
///
/// Instances are immutable pairings of the column objects supplied at creation
/// time.  Later reordering of an owning [Value] does not rewrite existing
/// pairings.
///
/// @since 9.0
@Internal
public record ForeignKeyColumnMapping(Column column, Column referencedColumn) implements Serializable {
}
