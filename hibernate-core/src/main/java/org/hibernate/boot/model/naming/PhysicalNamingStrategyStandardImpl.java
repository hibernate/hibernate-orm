/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

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
	public PhysicalName toPhysicalCatalogName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalSchemaName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalSequenceName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalTypeName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalPrimaryKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalForeignKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalUniqueKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalIndexName(LogicalName logicalName, PhysicalNamingContext context) {
		return logicalName == null ? null
				: context.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
	}

}
