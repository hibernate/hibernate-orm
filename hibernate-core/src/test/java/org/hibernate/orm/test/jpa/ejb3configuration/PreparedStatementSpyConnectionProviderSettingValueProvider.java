/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;

/**
 * @author Jan Schatteman
 */
public class PreparedStatementSpyConnectionProviderSettingValueProvider extends NonStringValueSettingProvider {
	@Override
	public String getKey() {
		return org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
	}

	@Override
	public Object getValue() {
		return new PreparedStatementSpyConnectionProvider( false, false );
	}
}