/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;

import org.hibernate.testing.orm.junit.SettingProvider;

public class OracleNestedTableSettingProvider implements SettingProvider.Provider<String> {
	@Override
	public String getSetting() {
		return Environment.getProperties().getProperty( JdbcSettings.DIALECT ).contains( "Oracle" )
				? "TABLE"
				: "ARRAY";
	}
}
