/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Steve Ebersole
 */
public class CustomNamingStrategy implements PhysicalNamingStrategy {
	@Override
	public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return Identifier.toIdentifier( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		if ( logicalName == null ) {
			return null;
		}
		return Identifier.toIdentifier( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier( logicalName.getText().toUpperCase( Locale.ROOT ), logicalName.isQuoted() );
	}
}
