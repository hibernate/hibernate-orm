/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.testing.orm.jdbc;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.SettingProvider;

public class DefaultSQLStatementInspectorSettingProvider implements SettingProvider.Provider<SQLStatementInspector> {

	@Override
	public SQLStatementInspector getSetting() {
		return new SQLStatementInspector();
	}
}
