/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.io.Serializable;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Standard implementation of the PhysicalNamingStrategy contract.
 *
 * @author Steve Ebersole
 */
public class PhysicalNamingStrategyStandardImpl implements PhysicalNamingStrategy, Serializable {
	/**
	 * Singleton access
	 */
	public static final PhysicalNamingStrategyStandardImpl INSTANCE = new PhysicalNamingStrategyStandardImpl();

	@Override
	public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment context) {
		return logicalName;
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment context) {
		return logicalName;
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context) {
		return logicalName;
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment context) {
		return logicalName;
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment context) {
		return logicalName;
	}
}
