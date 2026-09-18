/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.io.Serializable;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Standard implementation of the {@link PhysicalNamingStrategy} contract. This is a trivial implementation
 * where each physical name is taken to be exactly identical to the corresponding logical name.
 *
 * @author Steve Ebersole
 */
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class PhysicalNamingStrategyStandardImpl implements PhysicalNamingStrategy, Serializable {
	@SPI(SPI.Role.USE)
	public PhysicalNamingStrategyStandardImpl() {
	}

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
