/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.ejb3joincolumn;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;

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
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return toIdentifier( makeCleanIdentifier("tbl_" + logicalName.getText()), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return logicalName.getText().equals( "DTYPE" )
				? logicalName
				: toIdentifier( makeCleanIdentifier( "c_" + logicalName.getText() ), logicalName.isQuoted() );
	}

	private String makeCleanIdentifier(String s) {
		return s.substring( 0, Math.min(s.length(), 63) ).toLowerCase();
	}
}
