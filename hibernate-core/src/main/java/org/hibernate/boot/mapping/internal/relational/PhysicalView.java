/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Table reference for a persistent database view.
///
/// A view behaves like a persistent table reference for binding purposes, but it
/// originates from `@View` source metadata rather than from a normal table
/// annotation.  The record preserves the same logical/physical name split used
/// by [PhysicalTable].
///
/// @see org.hibernate.annotations.View
///
/// @since 9.0
/// @author Steve Ebersole
public record PhysicalView(
		LogicalName logicalName,
		LogicalName logicalCatalogName,
		LogicalName logicalSchemaName,
		org.hibernate.mapping.DatabaseView binding) implements PersistentTableReference {
	public PhysicalName physicalName() {
		return binding.getPhysicalName().objectName();
	}

	@Override
	public LogicalName logicalName() {
		return logicalName;
	}

	@Override
	public PhysicalName getPhysicalSchemaName() {
		return binding.getPhysicalName().schemaName();
	}

	@Override
	public LogicalName getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public PhysicalName getPhysicalCatalogName() {
		return binding.getPhysicalName().catalogName();
	}

	@Override
	public LogicalName getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public boolean exportable() {
		return true;
	}

	@Override
	public org.hibernate.mapping.DatabaseView binding() {
		return binding;
	}
}
