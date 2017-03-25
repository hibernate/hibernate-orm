/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.timestamp;

import java.util.Map;
import java.util.TimeZone;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL82Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.test.util.jdbc.TimeZoneConnectionProvider;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQL82Dialect.class)
public class JdbcTimestampUTCTimeZoneTest
		extends JdbcTimestampWithoutUTCTimeZoneTest {

	private TimeZoneConnectionProvider connectionProvider = new TimeZoneConnectionProvider(
			"America/Los_Angeles" );

	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		settings.put(
				AvailableSettings.JDBC_TIME_ZONE,
				TIME_ZONE
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	protected String expectedTimestampValue() {
		return "2000-01-01 00:00:00.000000";
	}
}

