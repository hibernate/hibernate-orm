/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Table reference for a physical table in the relational database.
///
/// The reference retains logical source names for binding lookup. Physical names
/// are owned by the mapping table and are accessed through that shared binding.
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
		LogicalName logicalName,
		LogicalName logicalCatalogName,
		LogicalName logicalSchemaName,
		org.hibernate.mapping.PhysicalTable binding) implements PhysicalTableReference {

	@Override
	public LogicalName logicalName() {
		return logicalName;
	}

	@Override
	public LogicalName getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public LogicalName getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public PhysicalName getPhysicalTableName() {
		return binding.getPhysicalName().objectName();
	}

	@Override
	public PhysicalName getPhysicalSchemaName() {
		return binding.getPhysicalName().schemaName();
	}

	@Override
	public PhysicalName getPhysicalCatalogName() {
		return binding.getPhysicalName().catalogName();
	}

	@Override
	public boolean exportable() {
		return !binding.isAbstract() && binding.getExportIdentifier() != null;
	}

	@Override
	public org.hibernate.mapping.PhysicalTable binding() {
		return binding;
	}
}
