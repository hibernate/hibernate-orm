/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.util.TimeZone;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.TimeZoneConnectionProvider;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQLDialect.class)
public class JdbcTimestampUTCTimeZoneTest extends JdbcTimestampWithoutUTCTimeZoneTest {

	private TimeZoneConnectionProvider connectionProvider;

	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		connectionProvider = new TimeZoneConnectionProvider(
				"America/Los_Angeles" );
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		builder.applySetting(
				AvailableSettings.JDBC_TIME_ZONE,
				TIME_ZONE
		);
	}

	@AfterAll
	protected void releaseResources() {
		if ( connectionProvider != null ) {
			connectionProvider.stop();
		}
	}

	protected String expectedTimestampValue() {
		return "2000-01-01 00:00:00.000000";
	}
}
