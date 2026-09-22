/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import java.util.Locale;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;

/**
 * @author Steve Ebersole
 */
public class CustomNamingStrategy implements PhysicalNamingStrategy {
	@Override
	@Nullable
	public PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalSequenceName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalColumnName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTypeName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

}
