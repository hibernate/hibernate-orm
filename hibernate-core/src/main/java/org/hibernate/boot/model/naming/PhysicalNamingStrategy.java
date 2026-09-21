/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Transforms logical mapping names into physical database names.
/// Use the supplied context's factory to construct results. A strategy may add
/// quoting, but Hibernate always preserves quoting requested by the logical name
/// and applies configured global and automatic quoting after this transformation.
/// Catalog/schema callbacks may supply defaults for absent logical qualifiers.
/// All other callbacks require and return a non-null name.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface PhysicalNamingStrategy {
	@Nullable PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, PhysicalNamingContext context);

	@Nullable PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, PhysicalNamingContext context);

	PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext context);

	PhysicalName toPhysicalSequenceName(LogicalName logicalName, PhysicalNamingContext context);

	PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext context);

	PhysicalName toPhysicalTypeName(LogicalName logicalName, PhysicalNamingContext context);

	/// Transform an explicit or generated primary-key constraint name.
	PhysicalName toPhysicalPrimaryKeyName(LogicalName logicalName, PhysicalNamingContext context);

	/// Transform an explicit or generated foreign-key constraint name.
	PhysicalName toPhysicalForeignKeyName(LogicalName logicalName, PhysicalNamingContext context);

	/// Transform an explicit or generated unique-key constraint name.
	PhysicalName toPhysicalUniqueKeyName(LogicalName logicalName, PhysicalNamingContext context);

	/// Transform an explicit or generated index name.
	PhysicalName toPhysicalIndexName(LogicalName logicalName, PhysicalNamingContext context);

}
