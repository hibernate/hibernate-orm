/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
