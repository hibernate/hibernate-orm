/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.mapping.PhysicalTable;

import java.util.List;

import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;

import jakarta.persistence.JoinColumn;

/// Table reference for a JPA secondary table bound to an entity.
///
/// The record keeps source-level logical names and delegates physical names to
/// the associated mapping table. That `PhysicalTable`
/// is created before its key can be completed; [#foreignKeySource] is retained so
/// the table-key and foreign-key phases can later apply the `@SecondaryTable`
/// key metadata.
///
/// @see jakarta.persistence.SecondaryTable
///
/// @since 9.0
/// @author Steve Ebersole
public record SecondaryTable(
		LogicalName logicalName,
		LogicalName logicalCatalogName,
		LogicalName logicalSchemaName,
		boolean optional,
		boolean owned,
		List<JoinColumn> primaryKeyJoinColumns,
		ForeignKeySource foreignKeySource,
		PhysicalTable binding) implements PhysicalTableReference {
	public PhysicalName physicalName() {
		return binding.getPhysicalName().objectName();
	}

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
		return !binding.isAbstract();
	}

	@Override
	public PhysicalTable binding() {
		return binding;
	}
}
