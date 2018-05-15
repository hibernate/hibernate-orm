/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.util.TimeZone;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * just sets a default TimeZone which is different than the current default one.
 *
 * @author Vlad Mihalcea
 */
public class TimeZoneConnectionProvider
		extends ConnectionProviderDelegate {

	private final String defaultTimeZone;
	private final String customTimeZone;

	public TimeZoneConnectionProvider(String customTimeZone) {
		this.customTimeZone = customTimeZone;
		this.defaultTimeZone =  System.setProperty( "user.timezone", customTimeZone);
		TimeZone.setDefault(TimeZone.getTimeZone( customTimeZone ));
	}

	@Override
	public void stop() {
		super.stop();
		System.setProperty( "user.timezone", defaultTimeZone);
		TimeZone.setDefault(TimeZone.getTimeZone( defaultTimeZone ));
	}
}
