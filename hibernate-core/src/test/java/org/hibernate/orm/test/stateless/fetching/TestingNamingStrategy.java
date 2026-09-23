/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.fetching;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import java.util.Locale;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.internal.util.StringHelper;

public class TestingNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	private final String prefix = determineUniquePrefix();

	protected String applyPrefix(String baseTableName) {
		String prefixed = prefix + '_' + baseTableName;
		return prefixed;
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
		return jdbcEnvironment.getPhysicalNameFactory().create( applyPrefix( logicalName.getText() ), logicalName.isQuoted() );
	}

	private String determineUniquePrefix() {
		return StringHelper.collapseQualifier( getClass().getName(), false ).toUpperCase( Locale.ROOT );
	}
}
