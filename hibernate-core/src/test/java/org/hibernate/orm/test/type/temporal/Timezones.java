/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.junit.jupiter.api.Assumptions;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Steve Ebersole
 */
public class Timezones {
	protected static final ZoneId ZONE_UTC_MINUS_8 = ZoneId.of( "UTC-8" );
	protected static final ZoneId ZONE_PARIS = ZoneId.of( "Europe/Paris" );
	protected static final ZoneId ZONE_GMT = ZoneId.of( "GMT" );
	protected static final ZoneId ZONE_OSLO = ZoneId.of( "Europe/Oslo" );
	protected static final ZoneId ZONE_AMSTERDAM = ZoneId.of( "Europe/Amsterdam" );
	protected static final ZoneId ZONE_AUCKLAND = ZoneId.of( "Pacific/Auckland" );
	protected static final ZoneId ZONE_SANTIAGO = ZoneId.of( "America/Santiago" );

	static TimeZone toTimeZone(ZoneId zoneId) {
		String idString = zoneId.getId();
		if ( idString.startsWith( "UTC+" ) || idString.startsWith( "UTC-" ) ) {
			// Apparently TimeZone doesn't understand UTC+XXX nor UTC-XXX
			// Using GMT+XXX or GMT-XXX as a fallback
			idString = "GMT" + idString.substring( "UTC".length() );
		}

		TimeZone result = TimeZone.getTimeZone( idString );
		if ( !idString.equals( result.getID() ) ) {
			// If the timezone is not understood, getTimeZone returns GMT and the condition above is true
			throw new IllegalStateException( "Attempting to test an unsupported timezone: " + zoneId );
		}

		return result;
	}

	public static void withDefaultTimeZone(Environment env, Runnable runnable) {
		TimeZone timeZoneBefore = TimeZone.getDefault();
		TimeZone.setDefault( toTimeZone( env.defaultJvmTimeZone() ) );
		SharedDriverManagerConnectionProvider.getInstance().onDefaultTimeZoneChange();
		/*
		 * Run the code in a new thread, because some libraries (looking at you, h2 JDBC driver)
		 * cache data dependent on the default timezone in thread local variables,
		 * and we want this data to be reinitialized with the new default time zone.
		 */
		try {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit( runnable );
			executor.shutdown();
			future.get();
		}
		catch (InterruptedException e) {
			throw new IllegalStateException( "Interrupted while testing", e );
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException ) {
				throw (RuntimeException) cause;
			}
			else if ( cause instanceof Error ) {
				throw (Error) cause;
			}
			else {
				throw new IllegalStateException( "Unexpected exception while testing", cause );
			}
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
			SharedDriverManagerConnectionProvider.getInstance().onDefaultTimeZoneChange();
		}
	}

	public static void assumeNoJdbcTimeZone(Environment env) {
		Assumptions.assumeTrue(
				env.hibernateJdbcTimeZone() == null,
				"Tests with native read/writes are only relevant when not using " + AvailableSettings.JDBC_TIME_ZONE
				+ ", because the expectations do not take that time zone into account."
				+ " When this property is set, we only test that a write by Hibernate followed by "
				+ " a read by Hibernate returns the same value."
		);
	}
}
