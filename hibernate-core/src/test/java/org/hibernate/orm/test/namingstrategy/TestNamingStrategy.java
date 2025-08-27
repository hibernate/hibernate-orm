/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

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
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		return toIdentifier(
				"PTCN_" + source.getAttributePath().getProperty(),
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment context) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalCatalogName( logicalName, context );
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSchemaName( logicalName, jdbcEnvironment );
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier( "TAB_" + logicalName.getText() );
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSequenceName( logicalName, jdbcEnvironment );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		if ( logicalName.getText().startsWith( "PTCN_" ) ) {
			return logicalName;
		}
		else {
			return Identifier.toIdentifier( "CN_" + logicalName.getText() );
		}
	}
}
