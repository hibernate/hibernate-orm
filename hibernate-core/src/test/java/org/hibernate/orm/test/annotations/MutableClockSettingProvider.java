/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.time.Clock;

import org.hibernate.testing.orm.junit.SettingProvider;

public final class MutableClockSettingProvider implements SettingProvider.Provider<Clock> {

	@Override
	public Clock getSetting() {
		return new MutableClock();
	}
}
