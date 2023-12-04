/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.PersistentTableReference;
import org.hibernate.mapping.Table;

/**
 * @see org.hibernate.annotations.View
 *
 * @author Steve Ebersole
 */
public record PhysicalView(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		Table table) implements PersistentTableReference {
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
}
