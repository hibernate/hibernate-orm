/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.ejb3joincolumn;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;


/**
 * @author Anton Wimmer
 * @author Steve Ebersole
 */
public class PhysicalNamingStrategyImpl extends PhysicalNamingStrategyStandardImpl {
	/**
	 * Singleton access
	 */
	public static final PhysicalNamingStrategyImpl INSTANCE = new PhysicalNamingStrategyImpl();

	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( makeCleanIdentifier("tbl_" + logicalName.getText()), logicalName.isQuoted() );
	}

	@Override
	public PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext jdbcEnvironment) {
		return logicalName.getText().equals( "DTYPE" )
				? jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() )
				: jdbcEnvironment.getPhysicalNameFactory().create( makeCleanIdentifier( "c_" + logicalName.getText() ), logicalName.isQuoted() );
	}

	private String makeCleanIdentifier(String s) {
		return s.substring( 0, Math.min(s.length(), 63) ).toLowerCase();
	}
}
