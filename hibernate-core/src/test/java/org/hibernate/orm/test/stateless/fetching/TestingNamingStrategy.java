/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.fetching;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;

public class TestingNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	private final String prefix = determineUniquePrefix();

	protected String applyPrefix(String baseTableName) {
		String prefixed = prefix + '_' + baseTableName;
		return prefixed;
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( applyPrefix( logicalName.getText() ) );
	}

	private String determineUniquePrefix() {
		return StringHelper.collapseQualifier( getClass().getName(), false ).toUpperCase( Locale.ROOT );
	}
}
