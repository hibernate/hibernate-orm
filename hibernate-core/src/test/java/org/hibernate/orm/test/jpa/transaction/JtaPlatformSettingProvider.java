/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Jan Schatteman
 */
public class JtaPlatformSettingProvider implements SettingProvider.Provider<TestingJtaPlatformImpl> {
	@Override
	public TestingJtaPlatformImpl getSetting() {
		return TestingJtaPlatformImpl.INSTANCE;
	}
}
