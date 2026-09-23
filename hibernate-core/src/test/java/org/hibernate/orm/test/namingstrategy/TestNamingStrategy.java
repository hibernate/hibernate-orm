/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nonnull
	public LogicalName determineBasicColumnName(@Nonnull BasicColumnNamingInput source, @Nonnull ImplicitNamingContext context) {
		return context.implicitName(
				"PTCN_" + AttributePath.parse( source.attributePath() ).getProperty() );
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalCatalogName( logicalName, context );
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSchemaName( logicalName, jdbcEnvironment );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( "TAB_" + logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalSequenceName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSequenceName( logicalName, jdbcEnvironment );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalColumnName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		if ( logicalName.getText().startsWith( "PTCN_" ) ) {
			return jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
		}
		else {
			return jdbcEnvironment.getPhysicalNameFactory().create( "CN_" + logicalName.getText(), logicalName.isQuoted() );
		}
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
