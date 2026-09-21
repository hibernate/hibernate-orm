/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak
 */
public class TestNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl implements PhysicalNamingStrategy {
	/**
	 * Singleton access
	 */
	public static final TestNamingStrategy INSTANCE = new TestNamingStrategy();

	public TestNamingStrategy() {
	}

	@Override
	public LogicalName determineBasicColumnName(BasicColumnNamingInput source, ImplicitNamingContext context) {
		return context.implicitName(
				"PTCN_" + AttributePath.parse( source.attributePath() ).getProperty() );
	}

	@Override
	public PhysicalName toPhysicalCatalogName(LogicalName logicalName, PhysicalNamingContext context) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalCatalogName( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalSchemaName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSchemaName( logicalName, jdbcEnvironment );
	}

	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( "TAB_" + logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalSequenceName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSequenceName( logicalName, jdbcEnvironment );
	}

	@Override
	public PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName.getText().startsWith( "PTCN_" ) ) {
			return logicalName == null ? null : jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
		}
		else {
			return jdbcEnvironment.getPhysicalNameFactory().create( "CN_" + logicalName.getText(), logicalName.isQuoted() );
		}
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
