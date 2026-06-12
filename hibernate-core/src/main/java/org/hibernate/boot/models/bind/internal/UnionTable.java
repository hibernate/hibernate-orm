/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.DenormalizedTable;

/// Table reference for a union-subclass denormalized table.
///
/// Union-subclass mappings use `DenormalizedTable` instances that are derived
/// from a base table reference.  The binder records the base reference so later
/// table and export decisions can distinguish the denormalized table from the
/// physical table it extends.
///
/// @since 9.0
/// @author Steve Ebersole
public record UnionTable(
		Identifier logicalName,
		TableReference base,
		DenormalizedTable binding,
		boolean exportable) implements TableReference {
}
