/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.PhysicalTableReference;
import org.hibernate.mapping.Table;

/// Table reference for a physical table in the relational database.
///
/// The binder keeps logical names and physical names together because source
/// annotations, implicit naming, physical naming, and `org.hibernate.mapping`
/// lookup often need to ask slightly different questions about the same table.
/// The [#binding()] is the mapping-model table shell created during table
/// binding; identifier-derived keys and foreign keys are completed by later
/// phases.
///
/// @see jakarta.persistence.Table
/// @see jakarta.persistence.CollectionTable
/// @see jakarta.persistence.JoinTable
///
/// @since 9.0
/// @author Steve Ebersole
public record PhysicalTable(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalTableName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		Table binding) implements PhysicalTableReference {

	@Override
	public Identifier logicalName() {
		return logicalName;
	}

	@Override
	public Identifier getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public Identifier getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public Identifier getPhysicalTableName() {
		return physicalTableName;
	}

	@Override
	public Identifier getPhysicalSchemaName() {
		return physicalSchemaName;
	}

	@Override
	public Identifier getPhysicalCatalogName() {
		return physicalCatalogName;
	}

	@Override
	public boolean exportable() {
		return !binding.isAbstract() && binding.getExportIdentifier() != null;
	}

	@Override
	public Table binding() {
		return binding;
	}
}
