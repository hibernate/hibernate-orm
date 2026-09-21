/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

/**
 * @author Emmanuel Bernard
 */
public class MyNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( "tbl_" + logicalName.getText(), logicalName.isQuoted() );
	}
}
