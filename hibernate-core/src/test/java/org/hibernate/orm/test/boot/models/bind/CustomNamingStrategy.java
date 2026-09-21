/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

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
	public PhysicalName toPhysicalCatalogName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalSchemaName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalSequenceName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalTypeName(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalPrimaryKeyName(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalForeignKeyName(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalUniqueKeyName(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalIndexName(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

}
