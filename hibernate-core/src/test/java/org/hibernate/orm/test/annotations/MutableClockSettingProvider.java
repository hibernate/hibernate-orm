/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
