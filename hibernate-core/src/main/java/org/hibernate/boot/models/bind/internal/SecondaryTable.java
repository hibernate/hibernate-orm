/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.PhysicalTableReference;
import org.hibernate.mapping.Table;

/**
 * @see jakarta.persistence.SecondaryTable
 *
 * @author Steve Ebersole
 */
public record SecondaryTable(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		boolean optional,
		boolean owned,
		Table table) implements PhysicalTableReference {
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
		return !table.isAbstract();
	}
}
