/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
