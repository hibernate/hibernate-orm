/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.time.Clock;
import java.time.Duration;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Helper for determining the correct clock for precision
 */
public class ClockHelper {

	private static final Duration TICK_8 = Duration.ofNanos( 10L );
	private static final Duration TICK_7 = Duration.ofNanos( 100L );
	private static final Duration TICK_6 = Duration.ofNanos( 1000L );
	private static final Duration TICK_5 = Duration.ofNanos( 10000L );
	private static final Duration TICK_4 = Duration.ofNanos( 100000L );
	private static final Duration TICK_3 = Duration.ofNanos( 1000000L );
	private static final Duration TICK_2 = Duration.ofNanos( 10000000L );
	private static final Duration TICK_1 = Duration.ofNanos( 100000000L );
	private static final Duration TICK_0 = Duration.ofNanos( 1000000000L );

	public static Clock forPrecision(Integer precision, SharedSessionContractImplementor session) {
		final int resolvedPrecision;
		if ( precision == null ) {
			resolvedPrecision = session.getJdbcServices().getDialect().getDefaultTimestampPrecision();
		}
		else {
			resolvedPrecision = precision;
		}
		final Clock clock = Clock.systemDefaultZone();
		switch ( resolvedPrecision ) {
			case 0:
				return Clock.tick( clock, TICK_0 );
			case 1:
				return Clock.tick( clock, TICK_1 );
			case 2:
				return Clock.tick( clock, TICK_2 );
			case 3:
				return Clock.tick( clock, TICK_3 );
			case 4:
				return Clock.tick( clock, TICK_4 );
			case 5:
				return Clock.tick( clock, TICK_5 );
			case 6:
				return Clock.tick( clock, TICK_6 );
			case 7:
				return Clock.tick( clock, TICK_7 );
			case 8:
				return Clock.tick( clock, TICK_8 );
			case 9:
				return clock;
		}
		throw new IllegalArgumentException( "Illegal precision: " + resolvedPrecision );
	}
}
