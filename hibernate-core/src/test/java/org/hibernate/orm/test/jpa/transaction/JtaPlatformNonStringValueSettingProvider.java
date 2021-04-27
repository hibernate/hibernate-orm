/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;

/**
 * @author Jan Schatteman
 */
public class JtaPlatformNonStringValueSettingProvider extends NonStringValueSettingProvider {
	@Override
	public String getKey() {
		return AvailableSettings.JTA_PLATFORM;
	}

	@Override
	public Object getValue() {
		return TestingJtaPlatformImpl.INSTANCE;
	}
}
