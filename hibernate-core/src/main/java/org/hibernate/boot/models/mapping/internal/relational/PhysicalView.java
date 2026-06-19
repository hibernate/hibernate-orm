/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;

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
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		Table binding) implements PersistentTableReference {
	@Override
	public Identifier logicalName() {
		return logicalName;
	}

	@Override
	public Identifier getPhysicalSchemaName() {
		return physicalSchemaName;
	}

	@Override
	public Identifier getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public Identifier getPhysicalCatalogName() {
		return physicalCatalogName;
	}

	@Override
	public Identifier getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public boolean exportable() {
		return true;
	}

	@Override
	public Table binding() {
		return binding;
	}
}
