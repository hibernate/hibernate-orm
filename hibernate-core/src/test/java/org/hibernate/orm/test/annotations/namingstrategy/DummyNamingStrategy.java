/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

public class DummyNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( "T" + logicalName.getText(), logicalName.isQuoted() );
	}
}
