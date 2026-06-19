/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.mapping.internal.sources.ForeignKeySource;
import org.hibernate.mapping.Table;

/// Table reference for a JPA secondary table bound to an entity.
///
/// The record keeps both the source-level logical identifiers and the physical
/// identifiers chosen during table binding.  The associated `org.hibernate.mapping.Table`
/// is created before its key can be completed; [#foreignKeySource] is retained so
/// the table-key and foreign-key phases can later apply the `@SecondaryTable`
/// key metadata.
///
/// @see jakarta.persistence.SecondaryTable
///
/// @since 9.0
/// @author Steve Ebersole
public record SecondaryTable(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		boolean optional,
		boolean owned,
		ForeignKeySource foreignKeySource,
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
		return physicalName;
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
		return !binding.isAbstract();
	}

	@Override
	public Table binding() {
		return binding;
	}
}
