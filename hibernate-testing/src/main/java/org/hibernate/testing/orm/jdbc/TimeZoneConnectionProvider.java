/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.jdbc;

import java.util.TimeZone;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;

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
		// Clear the connection pool to avoid issues with drivers that initialize the session TZ to the system TZ
		SharedDriverManagerConnectionProvider.getInstance().onDefaultTimeZoneChange();
	}

	@Override
	public void stop() {
		super.stop();
		System.setProperty( "user.timezone", defaultTimeZone);
		TimeZone.setDefault(TimeZone.getTimeZone( defaultTimeZone ));
		// Clear the connection pool to avoid issues with drivers that initialize the session TZ to the system TZ
		SharedDriverManagerConnectionProvider.getInstance().onDefaultTimeZoneChange();
	}
}
