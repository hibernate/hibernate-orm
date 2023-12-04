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
 * Models a physical table from the underlying database schema
 *
 * @see jakarta.persistence.Table
 * @see jakarta.persistence.CollectionTable
 * @see jakarta.persistence.JoinTable
 *
 * @author Steve Ebersole
 */
public record PhysicalTable(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalTableName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
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
		return !table.isAbstract() && table.getExportIdentifier() != null;
	}
}
