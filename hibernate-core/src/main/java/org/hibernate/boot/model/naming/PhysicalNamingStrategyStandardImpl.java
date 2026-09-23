/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Identity physical naming: preserve logical text and requested quoting.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class PhysicalNamingStrategyStandardImpl implements PhysicalNamingStrategy, Serializable {
	public static final PhysicalNamingStrategyStandardImpl INSTANCE = new PhysicalNamingStrategyStandardImpl();

	@SPI(SPI.Role.USE)
	public PhysicalNamingStrategyStandardImpl() {
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalSequenceName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalColumnName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTypeName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalIndexName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

}
