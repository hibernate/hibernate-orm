/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Transforms explicit and implicit logical names into physical database names.
///
/// Construct results using [PhysicalName.Factory#create(String, boolean)] on the
/// factory supplied by [PhysicalNamingContext#getPhysicalNameFactory()].
/// Pass the name's text without quote delimiters and specify quoting separately.
///
/// A strategy may add quoting, but may not remove quoting requested by the logical name;
/// Hibernate enforces this when finalizing a non-null result.
/// Hibernate may apply configured global and automatic quoting after the transformation.
///
/// Catalog and schema names are optional: their callbacks accept and may return null.
/// All other callbacks require non-null inputs and results.
/// The naming context is always non-null.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface PhysicalNamingStrategy {
	/// Transform the logical catalog name into a physical name.
	/// A strategy may supply a default when the logical name is absent.
	/// A null result supplies no catalog qualifier; SQL generation may still apply
	/// a configured default.
	///
	/// @param logicalName The logical catalog name, or null if absent
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical catalog name, or null to supply no catalog qualifier
	@Nullable
	PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical schema name into a physical name.
	/// A strategy may supply a default when the logical name is absent.
	/// A null result supplies no schema qualifier; SQL generation may still apply
	/// a configured default.
	///
	/// @param logicalName The logical schema name, or null if absent
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical schema name, or null to supply no schema qualifier
	@Nullable
	PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical table name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical table name; never null
	@Nonnull
	PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical sequence name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical sequence name; never null
	@Nonnull
	PhysicalName toPhysicalSequenceName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical column name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical column name; never null
	@Nonnull
	PhysicalName toPhysicalColumnName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical named SQL type name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical named SQL type name; never null
	@Nonnull
	PhysicalName toPhysicalTypeName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical primary-key constraint name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical primary-key constraint name; never null
	@Nonnull
	PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical foreign-key constraint name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical foreign-key constraint name; never null
	@Nonnull
	PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical unique-key constraint name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical unique-key constraint name; never null
	@Nonnull
	PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

	/// Transform the logical index name into a physical name.
	///
	/// @param logicalName The non-null explicit or implicit logical name
	/// @param context The non-null context supplying the physical-name factory
	/// @return The physical index name; never null
	@Nonnull
	PhysicalName toPhysicalIndexName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context);

}
