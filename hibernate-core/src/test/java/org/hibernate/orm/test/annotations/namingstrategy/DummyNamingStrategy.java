/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

public class DummyNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( "T" + logicalName.getText(), logicalName.isQuoted() );
	}
}
